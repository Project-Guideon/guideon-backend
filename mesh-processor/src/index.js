import express from 'express';
import { combine }   from './combiner.js';
import { stripRig }  from './stripRig.js';
import { convert }   from './convert.js';

const app  = express();
const PORT = process.env.PORT || 3001;

app.use(express.json());

/**
 * POST /combine
 * {
 *   "baseGlb":   "/app/uploads/{siteId}/{hash}.glb",  — Tripo rig_model 결과 GLB
 *   "animGlbs":  {
 *     "Idle":      "/app/uploads/{siteId}/{hash}.glb",
 *     "Talking":   "/app/uploads/{siteId}/{hash}.glb",
 *     "Listening": "/app/uploads/{siteId}/{hash}.glb",
 *     "Thinking":  "/app/uploads/{siteId}/{hash}.glb",
 *     "Waving":    "/app/uploads/{siteId}/{hash}.glb"
 *   },
 *   "outputGlb": "/app/uploads/{siteId}/anim_{generationId}.glb"
 * }
 *
 * 응답: { "success": true, "outputGlb": "..." }
 */
app.post('/combine', async (req, res) => {
  const { baseGlb, animGlbs, outputGlb } = req.body;

  if (!baseGlb || !animGlbs || !outputGlb) {
    return res.status(400).json({ error: 'baseGlb, animGlbs, outputGlb 필수' });
  }
  if (typeof animGlbs !== 'object' || Object.keys(animGlbs).length === 0) {
    return res.status(400).json({ error: 'animGlbs는 비어있지 않은 객체여야 합니다' });
  }

  try {
    await combine(baseGlb, animGlbs, outputGlb);
    res.json({ success: true, outputGlb });
  } catch (e) {
    console.error('[combine] 실패:', e.message);
    res.status(500).json({ error: e.message });
  }
});

/**
 * POST /strip-rig
 * {
 *   "riggedGlb": "/app/uploads/{siteId}/{hash}.glb",  — Tripo rig_model 결과 GLB
 *   "outputFbx": "/app/uploads/{siteId}/clean_mesh_{generationId}.fbx"
 * }
 *
 * 스켈레톤·스킨 제거 → T-포즈 clean mesh → FBX 변환 (Mixamo 업로드용)
 * 응답: { "success": true, "outputFbx": "..." }
 */
app.post('/strip-rig', async (req, res) => {
  const { riggedGlb, outputFbx } = req.body;

  if (!riggedGlb || !outputFbx) {
    return res.status(400).json({ error: 'riggedGlb, outputFbx 필수' });
  }

  try {
    await stripRig(riggedGlb, outputFbx);
    res.json({ success: true, outputFbx });
  } catch (e) {
    console.error('[strip-rig] 실패:', e.message);
    res.status(500).json({ error: e.message });
  }
});

/**
 * POST /convert
 * {
 *   "input":  "/app/uploads/{siteId}/rig_raw_{generationId}.fbx",  — Tripo rig_model FBX
 *   "output": "/app/uploads/{siteId}/mascot.glb"
 * }
 *
 * assimp 확장자 자동 추론으로 FBX→GLB 변환.
 * 응답: { "success": true, "output": "..." }
 */
app.post('/convert', async (req, res) => {
  const { input, output } = req.body;

  if (!input || !output) {
    return res.status(400).json({ error: 'input, output 필수' });
  }

  try {
    await convert(input, output);
    res.json({ success: true, output });
  } catch (e) {
    console.error('[convert] 실패:', e.message);
    res.status(500).json({ error: e.message });
  }
});

app.get('/health', (_, res) => res.json({ status: 'ok' }));

app.listen(PORT, () => console.log(`mesh-processor :${PORT}`));
