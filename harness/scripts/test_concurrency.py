#!/usr/bin/env python3
"""쿠폰 API에 동시 요청을 보내고 결과 정합성을 요약하는 표준 라이브러리 부하 도구."""

import argparse
import json
import threading
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.error import URLError
from urllib.request import Request, urlopen


def issue(base_url: str, coupon_id: int, user_id: int, timeout: float) -> str:
    payload = json.dumps({"userId": user_id}).encode("utf-8")
    request = Request(
        f"{base_url}/api/coupons/{coupon_id}/issue",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8")).get("status", "UNKNOWN")
    except (URLError, TimeoutError, OSError) as error:
        # 네트워크 오류도 누락시키지 않고 별도 결과로 집계해야 부하 결과가 왜곡되지 않습니다.
        return f"ERROR:{type(error).__name__}"


def main() -> int:
    parser = argparse.ArgumentParser(description="Run concurrent coupon issue requests")
    parser.add_argument("--base-url", default="http://localhost:8080")
    parser.add_argument("--coupon-id", type=int, required=True)
    parser.add_argument("--users", type=int, default=1000)
    parser.add_argument("--first-user-id", type=int, default=1)
    parser.add_argument("--workers", type=int, default=100)
    parser.add_argument("--expected-stock", type=int, default=100)
    parser.add_argument("--timeout", type=float, default=10.0)
    args = parser.parse_args()

    started = time.perf_counter()
    counts = {}
    with ThreadPoolExecutor(max_workers=args.workers) as executor:
        futures = [
            executor.submit(issue, args.base_url.rstrip("/"), args.coupon_id, args.first_user_id + offset, args.timeout)
            for offset in range(args.users)
        ]
        for future in as_completed(futures):
            status = future.result()
            counts[status] = counts.get(status, 0) + 1

    elapsed = time.perf_counter() - started
    success = counts.get("SUCCESS", 0)
    errors = sum(value for key, value in counts.items() if key.startswith("ERROR:"))
    result = {
        "couponId": args.coupon_id,
        "requestedUsers": args.users,
        "resultCounts": counts,
        "successCount": success,
        "overIssueCount": max(0, success - args.expected_stock),
        "networkErrorCount": errors,
        "elapsedSeconds": round(elapsed, 3),
    }
    print(json.dumps(result, indent=2, sort_keys=True))

    # 사용자 ID가 모두 고유한 입력에서는 성공 수가 초기 재고를 넘지 않아야 합니다.
    return 1 if errors or success > args.expected_stock else 0


if __name__ == "__main__":
    raise SystemExit(main())
