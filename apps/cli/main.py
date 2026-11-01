import os

import httpx

API_URL = os.environ.get("BIH_API_URL", "http://127.0.0.1:8000")


def main():
    print("B.I.H. CLI (type 'exit' to quit)")
    while True:
        try:
            msg = input("You: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nTake care.")
            break
        if not msg:
            continue
        if msg.lower() in {"exit", "quit"}:
            print("Take care.")
            break
        try:
            r = httpx.post(f"{API_URL}/chat", json={"message": msg}, timeout=20)
            r.raise_for_status()
            print("B.I.H.:", r.json().get("reply", ""))
        except httpx.ConnectError:
            print(
                "Error: cannot reach the B.I.H. API. Is it running?\n"
                "Start it with: uvicorn app.main:app --port 8000"
            )
        except Exception as e:
            print("Error:", e)


if __name__ == "__main__":
    main()
