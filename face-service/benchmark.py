"""
Benchmark script for InsightFace models on Face Service.
Evaluates 4 configurations:
- buffalo_l, det 640
- buffalo_l, det 320
- buffalo_s, det 640
- buffalo_s, det 320

Measures:
- p50 (ms)
- p95 (ms)
- RAM RSS (MB)
"""
import os
import sys
import time
import gc
import argparse
from pathlib import Path
import cv2
import numpy as np
import psutil

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


CONFIGS = [
    {"name": "buffalo_l, det 640", "model": "buffalo_l", "det_size": (640, 640)},
    {"name": "buffalo_l, det 320", "model": "buffalo_l", "det_size": (320, 320)},
    {"name": "buffalo_s, det 640", "model": "buffalo_s", "det_size": (640, 640)},
    {"name": "buffalo_s, det 320", "model": "buffalo_s", "det_size": (320, 320)},
]


def get_default_image_path() -> Path:
    # Use sample image included in insightface package or local sample
    local_sample = Path(__file__).resolve().parent / "sample.jpg"
    if local_sample.exists():
        return local_sample
    
    pkg_sample = (
        Path(__file__).resolve().parent
        / ".venv"
        / "Lib"
        / "site-packages"
        / "insightface"
        / "data"
        / "images"
        / "t1.jpg"
    )
    if pkg_sample.exists():
        return pkg_sample

    raise FileNotFoundError("Không tìm thấy ảnh mẫu để benchmark.")


def run_benchmark(
    image_path: Path,
    num_iterations: int = 100,
    warmup: int = 10,
    threads: int = None,
):
    if threads:
        os.environ["OMP_NUM_THREADS"] = str(threads)
        os.environ["MKL_NUM_THREADS"] = str(threads)
        print(f"Đã đặt giới hạn luồng OMP_NUM_THREADS = {threads}")

    img = cv2.imread(str(image_path))
    if img is None:
        raise ValueError(f"Không thể đọc ảnh: {image_path}")

    print("=" * 70)
    print(f"BẮT ĐẦU BENCHMARK ({num_iterations} lượt lặp, warm-up {warmup} lượt)")
    print(f"Ảnh test: {image_path} (Kích thước: {img.shape[1]}x{img.shape[0]})")
    print("=" * 70)

    process = psutil.Process()
    results = []

    for cfg in CONFIGS:
        cfg_name = cfg["name"]
        model = cfg["model"]
        det_size = cfg["det_size"]

        print(f"\n--> Đang chạy cấu hình: {cfg_name}...")
        gc.collect()

        # Khởi tạo mô hình
        engine = FaceEngine(model=model, det_size=det_size)

        # Warm-up
        for _ in range(warmup):
            engine.analyze(img)

        # Chạy đo thời gian
        times = []
        for i in range(num_iterations):
            t_start = time.perf_counter()
            engine.analyze(img)
            times.append((time.perf_counter() - t_start) * 1000.0)

        # Đo RAM RSS (MB)
        ram_mb = process.memory_info().rss / (1024 * 1024)

        p50 = float(np.percentile(times, 50))
        p95 = float(np.percentile(times, 95))

        note = "Độ chính xác cao, nhận diện tốt" if "buffalo_l" in model else "Nhẹ, tốc độ nhanh"
        if det_size == (320, 320):
            note += " (det nhỏ tiết kiệm CPU)"

        results.append({
            "config": cfg_name,
            "p50": p50,
            "p95": p95,
            "ram_mb": ram_mb,
            "note": note
        })

        print(f"    p50: {p50:.2f} ms | p95: {p95:.2f} ms | RAM: {ram_mb:.1f} MB")

        # Giải phóng engine trước khi chuyển sang cấu hình tiếp theo
        del engine
        gc.collect()

    print("\n" + "=" * 70)
    print("KẾT QUẢ BENCHMARK (Copy bảng dưới đây vào báo cáo):")
    print("=" * 70)
    table_header = "| Cấu hình | p50 (ms) | p95 (ms) | RAM (MB) | Ghi chú |"
    table_separator = "|---|---|---|---|---|"
    print(table_header)
    print(table_separator)
    for r in results:
        print(f"| {r['config']} | {r['p50']:.1f} | {r['p95']:.1f} | {r['ram_mb']:.1f} | {r['note']} |")
    print("=" * 70)

    return results


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="InsightFace Benchmark Script")
    parser.add_argument("--image", type=str, default=None, help="Đường dẫn tới ảnh test")
    parser.add_argument("--iterations", type=int, default=100, help="Số lần chạy đo (mặc định: 100)")
    parser.add_argument("--warmup", type=int, default=10, help="Số lần warm-up (mặc định: 10)")
    parser.add_argument("--threads", type=int, default=None, help="Giới hạn số luồng OMP_NUM_THREADS")
    args = parser.parse_args()

    test_img = Path(args.image) if args.image else get_default_image_path()
    run_benchmark(
        image_path=test_img,
        num_iterations=args.iterations,
        warmup=args.warmup,
        threads=args.threads
    )
