import { execSync } from 'child_process';

/**
 * assimp export를 사용한 포맷 변환.
 * assimp는 확장자로 포맷을 자동 추론하므로 FBX→GLB, GLB→FBX 모두 동일하게 호출.
 *
 * 주 용도: Tripo animate_rig가 FBX를 반환한 경우 GLB로 변환.
 *
 * @param {string} inputPath  입력 파일 경로 (예: /app/uploads/1/rig_raw_42.fbx)
 * @param {string} outputPath 출력 파일 경로 (예: /app/uploads/1/mascot.glb)
 */
export async function convert(inputPath, outputPath) {
  console.log(`[convert] ${inputPath} → ${outputPath}`);
  execSync(`assimp export "${inputPath}" "${outputPath}"`, { timeout: 60000 });
  console.log(`[convert] 변환 완료: ${outputPath}`);
}
