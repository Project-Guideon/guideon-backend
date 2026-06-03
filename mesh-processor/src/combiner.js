import { NodeIO }         from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';

const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);

/**
 * Tripo rigged GLB + Mixamo 애니메이션 GLB 5개 → 통합 anim GLB
 *
 * 핵심 원리:
 *   Mixamo "Without Skin" GLB의 본 이름(mixamorig:Hips 등)이
 *   Tripo spec:mixamo GLB의 본 이름과 동일하므로,
 *   이름 기준으로 채널 타깃을 remapping하여 베이스 문서에 붙인다.
 *
 * @param {string} baseGlbPath  Tripo rig_model 결과 GLB 경로
 * @param {Object} animGlbs     { "Idle": "/path/idle.glb", "Talking": "/path/talking.glb", ... }
 *                              KEY = GLB에 임베드될 클립명, VALUE = 파일 경로
 * @param {string} outputPath   출력 GLB 경로
 */
export async function combine(baseGlbPath, animGlbs, outputPath) {
  console.log(`[combine] base: ${baseGlbPath}`);

  // 1. 베이스 문서 로드 (Tripo 리깅 GLB — 메쉬 + 스켈레톤 포함)
  const baseDoc  = await io.read(baseGlbPath);
  const baseRoot = baseDoc.getRoot();

  // 2. 베이스 스켈레톤의 본 이름 → 노드 맵 구성
  const nodeByName = new Map();
  for (const node of baseRoot.listNodes()) {
    nodeByName.set(node.getName(), node);
  }
  console.log(`[combine] 베이스 본 ${nodeByName.size}개 인식`);

  // 3. 각 애니메이션 GLB에서 클립 추출 → 베이스에 이식
  for (const [clipName, animPath] of Object.entries(animGlbs)) {
    console.log(`[combine] 클립 병합: ${clipName} ← ${animPath}`);

    const animDoc  = await io.read(animPath);
    const animRoot = animDoc.getRoot();

    for (const srcAnim of animRoot.listAnimations()) {
      const newAnim = baseDoc.createAnimation(clipName);
      let channelCount = 0;

      for (const channel of srcAnim.listChannels()) {
        const srcTarget = channel.getTargetNode();
        if (!srcTarget) continue;

        // 본 이름으로 베이스 문서의 동일한 본 찾기
        const baseTarget = nodeByName.get(srcTarget.getName());
        if (!baseTarget) continue; // 매칭 본 없으면 skip

        const srcSampler = channel.getSampler();
        const srcInput   = srcSampler.getInput();
        const srcOutput  = srcSampler.getOutput();

        // Accessor 데이터 복사 (참조가 아닌 deep copy — 문서 간 공유 불가)
        const newInput = baseDoc.createAccessor()
          .setType(srcInput.getType())
          .setComponentType(srcInput.getComponentType())
          .setArray(srcInput.getArray().slice());

        const newOutput = baseDoc.createAccessor()
          .setType(srcOutput.getType())
          .setComponentType(srcOutput.getComponentType())
          .setArray(srcOutput.getArray().slice());

        const newSampler = baseDoc.createAnimationSampler()
          .setInput(newInput)
          .setOutput(newOutput)
          .setInterpolation(srcSampler.getInterpolation());

        const newChannel = baseDoc.createAnimationChannel()
          .setSampler(newSampler)
          .setTargetNode(baseTarget)          // ← 베이스 문서의 본으로 교체
          .setTargetPath(channel.getTargetPath());

        newAnim.addSampler(newSampler);
        newAnim.addChannel(newChannel);
        channelCount++;
      }

      console.log(`  ${clipName}: ${channelCount}개 채널 이식 완료`);
    }
  }

  // 4. 통합 GLB 출력
  await io.write(outputPath, baseDoc);
  console.log(`[combine] 완료: ${outputPath}`);
}
