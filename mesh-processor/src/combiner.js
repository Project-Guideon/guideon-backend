import { NodeIO }         from '@gltf-transform/core';
import { ALL_EXTENSIONS } from '@gltf-transform/extensions';

const io = new NodeIO().registerExtensions(ALL_EXTENSIONS);

// Mixamo "Without Skin" 본 이름 → Tripo spec:mixamo 본 이름 매핑.
// Mixamo는 "mixamorig:" 접두어를 사용하고 Tripo는 자체 이름 규칙을 따르므로
// 직접 이름 일치가 안 되는 경우 이 테이블로 폴백한다.
const MIXAMO_TO_TRIPO = {
  'mixamorig:Hips':         'Pelvis',
  'mixamorig:Spine':        'Waist',
  'mixamorig:Spine1':       'Spine01',
  'mixamorig:Spine2':       'Spine02',
  'mixamorig:Neck':         'NeckTwist01',
  'mixamorig:Head':         'Head',
  'mixamorig:LeftUpLeg':    'L_Thigh',
  'mixamorig:LeftLeg':      'L_Calf',
  'mixamorig:LeftFoot':     'L_Foot',
  'mixamorig:LeftToeBase':  'L_ToeBase',
  'mixamorig:RightUpLeg':   'R_Thigh',
  'mixamorig:RightLeg':     'R_Calf',
  'mixamorig:RightFoot':    'R_Foot',
  'mixamorig:RightToeBase': 'R_ToeBase',
  'mixamorig:LeftShoulder': 'L_Clavicle',
  'mixamorig:LeftArm':      'L_Upperarm',
  'mixamorig:LeftForeArm':  'L_Forearm',
  'mixamorig:LeftHand':     'L_Hand',
  'mixamorig:RightShoulder':'R_Clavicle',
  'mixamorig:RightArm':     'R_Upperarm',
  'mixamorig:RightForeArm': 'R_Forearm',
  'mixamorig:RightHand':    'R_Hand',
};

/**
 * Tripo rigged GLB + Mixamo 애니메이션 GLB 5개 → 통합 anim GLB
 *
 * 채널 타깃 매핑 우선순위:
 *   1. 직접 이름 일치 (base 본 이름 = anim 본 이름)
 *   2. MIXAMO_TO_TRIPO 테이블 폴백 (mixamorig:X → Tripo 이름)
 *
 * @param {string} baseGlbPath  Tripo rig_model 결과 GLB 경로
 * @param {Object} animGlbs     { "Idle": "/path/idle.glb", "Talking": "/path/talking.glb", ... }
 *                              KEY = GLB에 임베드될 클립명, VALUE = 파일 경로
 * @param {string} outputPath   출력 GLB 경로
 */
export async function combine(baseGlbPath, animGlbs, outputPath) {
  console.log(`[combine] base: ${baseGlbPath}`);

  const baseDoc  = await io.read(baseGlbPath);
  const baseRoot = baseDoc.getRoot();

  const nodeByName = new Map();
  for (const node of baseRoot.listNodes())
    nodeByName.set(node.getName(), node);
  console.log(`[combine] 베이스 본 ${nodeByName.size}개 인식`);

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

        const srcName = srcTarget.getName();

        // 1. 직접 이름 일치
        let baseTarget = nodeByName.get(srcName);

        // 2. Mixamo → Tripo 이름 변환 폴백
        if (!baseTarget) {
          const tripoName = MIXAMO_TO_TRIPO[srcName];
          if (tripoName) baseTarget = nodeByName.get(tripoName);
        }

        if (!baseTarget) continue;

        const srcSampler = channel.getSampler();
        const srcInput   = srcSampler.getInput();
        const srcOutput  = srcSampler.getOutput();

        // gltf-transform v4: setComponentType 제거됨 — TypedArray 타입이 컴포넌트 타입을 결정
        const newInput = baseDoc.createAccessor();
        newInput.setType(srcInput.getType());
        newInput.setArray(srcInput.getArray().slice());

        const newOutput = baseDoc.createAccessor();
        newOutput.setType(srcOutput.getType());
        newOutput.setArray(srcOutput.getArray().slice());

        const newSampler = baseDoc.createAnimationSampler();
        newSampler.setInput(newInput);
        newSampler.setOutput(newOutput);
        newSampler.setInterpolation(srcSampler.getInterpolation());

        const newChannel = baseDoc.createAnimationChannel();
        newChannel.setSampler(newSampler);
        newChannel.setTargetNode(baseTarget);
        newChannel.setTargetPath(channel.getTargetPath());

        newAnim.addSampler(newSampler);
        newAnim.addChannel(newChannel);
        channelCount++;
      }

      console.log(`  ${clipName}: ${channelCount}개 채널 이식 완료`);
    }
  }

  await io.write(outputPath, baseDoc);
  console.log(`[combine] 완료: ${outputPath}`);
}
