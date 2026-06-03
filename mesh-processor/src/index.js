import express from 'express';
import { combine } from './combiner.js';

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

app.get('/health', (_, res) => res.json({ status: 'ok' }));

app.listen(PORT, () => console.log(`mesh-processor :${PORT}`));
