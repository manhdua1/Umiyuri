"""
Evaluation script for Face Verification (FAR / FRR analysis).
Reads dataset structured as:
  data/
    <person_1>/
      img1.jpg
      img2.jpg
    <person_2>/
      img1.jpg
      img2.jpg

Computes:
- Genuine similarity pairs (same identity)
- Impostor similarity pairs (different identities)
- FAR (False Acceptance Rate) and FRR (False Rejection Rate) across thresholds (0.2 -> 0.8, step 0.02)
"""
import os
import sys
import argparse
import itertools
from pathlib import Path
import cv2
import numpy as np

# Ensure UTF-8 stdout on Windows
if sys.platform == "win32":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    except Exception:
        pass

# Ensure face_engine can be imported
sys.path.insert(0, str(Path(__file__).resolve().parent))
from face_engine import FaceEngine


def resolve_data_path(user_arg: str) -> Path:
    """Tự động tìm thư mục data tại đường dẫn chỉ định hoặc các vị trí mặc định."""
    candidate_paths = [
        Path(user_arg),
        Path(__file__).resolve().parent / user_arg,
        Path(__file__).resolve().parent.parent / user_arg,
    ]
    for p in candidate_paths:
        if p.exists() and p.is_dir():
            return p
    return Path(__file__).resolve().parent / user_arg


def extract_embeddings_from_directory(data_dir: Path, engine: FaceEngine, max_people: int = None):
    """
    Duyệt thư mục data_dir theo cấu trúc: data/<tên người>/*.jpg
    Trích xuất vector embedding 512 chiều đã chuẩn hóa L2 cho từng ảnh.
    """
    embs = {}  # {person_name: [vector1, vector2, ...]}
    valid_exts = {".jpg", ".jpeg", ".png", ".webp"}

    person_dirs = [d for d in data_dir.iterdir() if d.is_dir() and not d.name.startswith(".")]
    if max_people and max_people > 0:
        person_dirs = person_dirs[:max_people]

    if not person_dirs:
        return embs

    print(f"Đang duyệt {len(person_dirs)} người trong thư mục '{data_dir}'...", flush=True)
    total_images_processed = 0

    for pdir in person_dirs:
        person_name = pdir.name
        embs[person_name] = []
        image_files = [f for f in pdir.iterdir() if f.is_file() and f.suffix.lower() in valid_exts]

        for img_path in image_files:
            img = cv2.imread(str(img_path))
            if img is None:
                continue

            faces = engine.analyze(img)
            if not faces:
                continue

            best_face = max(faces, key=lambda f: f.det_score)
            vec = best_face.normed_embedding
            if vec is None:
                vec = best_face.embedding / np.linalg.norm(best_face.embedding)

            embs[person_name].append(np.array(vec, dtype=np.float32))
            total_images_processed += 1

    # Lọc những người có ít nhất 1 ảnh trích xuất thành công
    valid_embs = {k: v for k, v in embs.items() if len(v) > 0}
    print(f"Hoàn thành trích xuất {total_images_processed} ảnh ({len(valid_embs)} người hợp lệ).\n", flush=True)
    return valid_embs


def evaluate_thresholds(embs: dict):
    """
    Tính genuine, impostor dot product và quét ngưỡng t từ 0.2 đến 0.8 (step 0.02).
    """
    genuine, impostor = [], []
    people = list(embs.items())                      # {tên: [vec, ...]}

    for name, vs in people:
        genuine += [a @ b for a, b in itertools.combinations(vs, 2)]

    for (n1, v1), (n2, v2) in itertools.combinations(people, 2):
        impostor += [a @ b for a in v1 for b in v2]

    if not genuine:
        print("[CẢNH BÁO] Không có đủ ảnh cùng một người để tạo cặp Genuine (cần mỗi người >= 2 ảnh).", flush=True)
        return

    if not impostor:
        print("[CẢNH BÁO] Không có đủ người khác nhau để tạo cặp Impostor (cần ít nhất 2 người).", flush=True)
        return

    genuine, impostor = np.array(genuine), np.array(impostor)

    print("=" * 55, flush=True)
    print(f"TỔNG HỢP CẶP ĐÁNH GIÁ:", flush=True)
    print(f"- Số cặp Genuine  (cùng người) : {len(genuine)}", flush=True)
    print(f"- Số cặp Impostor (khác người) : {len(impostor)}", flush=True)
    print(f"- Similarity Genuine : Min={genuine.min():.3f}, Mean={genuine.mean():.3f}, Max={genuine.max():.3f}", flush=True)
    print(f"- Similarity Impostor: Min={impostor.min():.3f}, Mean={impostor.mean():.3f}, Max={impostor.max():.3f}", flush=True)
    print("=" * 55, flush=True)

    print("\nKẾT QUẢ QUÉT NGƯỠNG FAR & FRR:", flush=True)
    print("-" * 35, flush=True)
    print(f"{'t':<6} {'FAR':<12} {'FRR':<12}", flush=True)
    print("-" * 35, flush=True)

    best_t = None
    min_diff = float("inf")

    for t in np.arange(0.2, 0.8, 0.02):
        frr = np.mean(genuine < t)
        far = np.mean(impostor >= t)
        print(f"{t:.2f}  FAR={far:.3f}  FRR={frr:.3f}", flush=True)

        diff = abs(far - frr)
        if diff < min_diff:
            min_diff = diff
            best_t = t

    print("-" * 35, flush=True)
    if best_t is not None:
        best_far = np.mean(impostor >= best_t)
        best_frr = np.mean(genuine < best_t)
        print(f"\n💡 Ngưỡng cân bằng tối ưu (EER gần nhất):", flush=True)
        print(f"   t = {best_t:.2f}  -->  FAR = {best_far:.3f}, FRR = {best_frr:.3f}", flush=True)


def run_demo_evaluation():
    print("[CHẾ ĐỘ DEMO] Đang tạo dữ liệu giả lập (3 người, mỗi người 3 ảnh)...", flush=True)
    np.random.seed(42)
    embs = {}
    for person_id in ["person_1", "person_2", "person_3"]:
        base_vec = np.random.randn(512)
        base_vec /= np.linalg.norm(base_vec)
        person_vectors = []
        for _ in range(3):
            noisy_vec = base_vec + np.random.normal(0, 0.25, size=512)
            noisy_vec /= np.linalg.norm(noisy_vec)
            person_vectors.append(noisy_vec)
        embs[person_id] = person_vectors

    evaluate_thresholds(embs)


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Face Verification Evaluation (FAR/FRR)")
    parser.add_argument("--data-dir", type=str, default="data", help="Đường dẫn thư mục dataset (mặc định: data)")
    parser.add_argument("--model", type=str, default="buffalo_l", help="Mô hình InsightFace (buffalo_l / buffalo_s)")
    parser.add_argument("--det-size", type=int, default=640, help="Kích thước detection (640 hoặc 320)")
    parser.add_argument("--max-people", type=int, default=None, help="Giới hạn số người đọc để tăng tốc test")
    parser.add_argument("--demo", action="store_true", help="Chạy demo với dữ liệu giả lập")
    args = parser.parse_args()

    data_path = resolve_data_path(args.data_dir)

    if args.demo or not data_path.exists() or not any(data_path.iterdir()):
        if not args.demo:
            print(f"Thư mục '{data_path}' hiện chưa có dữ liệu.", flush=True)
            print("Chạy demo giả lập để kiểm tra quy trình tính toán:\n", flush=True)
        run_demo_evaluation()
    else:
        print(f"Khởi tạo FaceEngine ({args.model}, det_size={args.det_size})...", flush=True)
        engine = FaceEngine(model=args.model, det_size=(args.det_size, args.det_size))
        embeddings = extract_embeddings_from_directory(data_path, engine, max_people=args.max_people)
        evaluate_thresholds(embeddings)
