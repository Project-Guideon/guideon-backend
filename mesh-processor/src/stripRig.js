import { execSync } from 'child_process';

/**
 * GLB → FBX 변환 (Mixamo 업로드용).
 *
 * 입력: Tripo image_to_model 결과 GLB (뼈대 없는 clean mesh)
 * 출력: FBX (Mixamo 자동 리깅 전용)
 *
 * image_to_model 단계의 GLB는 이미 뼈대가 없으므로
 * gltf-transform 처리 없이 assimp 변환만 수행한다.
 *
 * @param {string} inputGlbPath  Tripo pre-rig 모델 GLB 경로
 * @param {string} outputFbxPath 출력 FBX 경로
 */
export async function stripRig(inputGlbPath, outputFbxPath) {
  console.log(`[stripRig] ${inputGlbPath} → ${outputFbxPath}`);
  execSync(`assimp export "${inputGlbPath}" "${outputFbxPath}"`, { timeout: 30000 });
  console.log(`[stripRig] FBX 변환 완료: ${outputFbxPath}`);
}
