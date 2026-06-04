import { NodeIO }         from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';
import { prune }          from '@gltf-transform/functions';
import { execSync }       from 'child_process';
import { writeFileSync, unlinkSync, existsSync } from 'fs';
import { tmpdir }         from 'os';
import { join }           from 'path';

const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);

/**
 * Tripo rigged GLB에서 스켈레톤을 제거하고 Mixamo-ready FBX를 생성.
 *
 * 원리:
 *   - Animation 제거 → 불필요한 클립 제거
 *   - Skin 제거 → 메쉬가 뼈대에서 분리되고 버텍스가 bind pose(T-포즈) 위치 유지
 *   - 메쉬 없는 orphan 노드(뼈대 노드) 제거
 *   - prune()으로 고아 리소스 정리
 *   - assimp CLI로 clean GLB → FBX 변환
 *
 * @param {string} inputGlbPath   Tripo rigged GLB 경로
 * @param {string} outputFbxPath  출력 FBX 경로
 */
export async function stripRig(inputGlbPath, outputFbxPath) {
  console.log(`[stripRig] input: ${inputGlbPath}`);

  const doc  = await io.read(inputGlbPath);
  const root = doc.getRoot();

  // 1. 모든 애니메이션 제거
  const animCount = root.listAnimations().length;
  root.listAnimations().forEach(anim => anim.dispose());
  console.log(`[stripRig] 애니메이션 ${animCount}개 제거`);

  // 2. 모든 Skin 제거 (메쉬→뼈대 바인딩 해제, 버텍스는 bind pose 유지)
  const skinCount = root.listSkins().length;
  root.listSkins().forEach(skin => skin.dispose());
  console.log(`[stripRig] Skin ${skinCount}개 제거`);

  // 3. 메쉬가 없는 노드(뼈대 노드) 제거
  //    씬 루트에 직접 연결된 노드는 유지, 내부 orphan 뼈대 노드만 제거
  const sceneRootNodes = new Set(
    root.listScenes().flatMap(s => s.listChildren())
  );
  let removedBones = 0;
  for (const node of root.listNodes()) {
    if (sceneRootNodes.has(node)) continue;
    if (node.getMesh() === null && node.getCamera() === null && node.getLight() === null) {
      node.dispose();
      removedBones++;
    }
  }
  console.log(`[stripRig] 뼈대 노드 ${removedBones}개 제거`);

  // 4. 고아 accessor/bufferView 등 정리
  await doc.transform(prune());

  // 5. 임시 GLB 파일로 저장
  const tmpGlb = join(tmpdir(), `clean_${Date.now()}.glb`);
  await io.write(tmpGlb, doc);
  console.log(`[stripRig] 임시 GLB 저장: ${tmpGlb}`);

  // 6. assimp CLI로 GLB → FBX 변환
  try {
    execSync(`assimp export "${tmpGlb}" "${outputFbxPath}"`, { timeout: 30000 });
    console.log(`[stripRig] FBX 변환 완료: ${outputFbxPath}`);
  } finally {
    if (existsSync(tmpGlb)) unlinkSync(tmpGlb);
  }
}
