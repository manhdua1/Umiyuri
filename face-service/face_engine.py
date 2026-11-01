# face_engine.py
import cv2, numpy as np
from insightface.app import FaceAnalysis

class FaceEngine:
    def __init__(self, model="buffalo_l", det_size=(640, 640)):
        self.app = FaceAnalysis(
            name=model,
            providers=["CPUExecutionProvider"],
            allowed_modules=["detection", "recognition"],  # bỏ module không dùng cho nhanh
        )
        self.app.prepare(ctx_id=-1, det_size=det_size)

    @staticmethod
    def decode(data: bytes):
        img = cv2.imdecode(np.frombuffer(data, np.uint8), cv2.IMREAD_COLOR)  # BGR
        if img is None:
            raise ValueError("invalid image")
        return img

    def analyze(self, img):
        return self.app.get(img)