# main.py
from fastapi import FastAPI, UploadFile, File, HTTPException
from face_engine import FaceEngine

app = FastAPI()
engine = FaceEngine()

@app.get("/health")
def health():
    return {"status": "ok"}

@app.post("/detect")
def detect(file: UploadFile = File(...)):
    img = engine.decode(file.file.read())
    faces = engine.analyze(img)
    return {"count": len(faces),
            "faces": [{"bbox": f.bbox.tolist(), "score": float(f.det_score)} for f in faces]}

@app.post("/embed")
def embed(file: UploadFile = File(...)):
    img = engine.decode(file.file.read())
    faces = engine.analyze(img)
    if not faces:
        raise HTTPException(422, "NO_FACE")
    if len(faces) > 1:
        raise HTTPException(422, "MULTIPLE_FACES")
    f = faces[0]

    return {"embedding": f.normed_embedding.tolist(),
            "det_score": float(f.det_score),
            "bbox": f.bbox.tolist()}