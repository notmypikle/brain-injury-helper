import httpx

API_URL = "http://127.0.0.1:8000"


def main():
    print("B.I.H. CLI (type 'exit' to quit)")
    while True:
        msg = input("You: ").strip()
        if msg.lower() in {"exit", "quit"}:
            print("Take care.")
            break
        try:
            r = httpx.post(f"{API_URL}/chat", json={"message": msg}, timeout=20)
            r.raise_for_status()
            print("B.I.H.:", r.json().get("reply", ""))
        except Exception as e:
            print("Error:", e)


if __name__ == "__main__":
    main()
