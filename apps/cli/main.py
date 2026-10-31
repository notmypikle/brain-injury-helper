import httpx

API_URL = "http://127.0.0.1:8000"


def call_chat(message: str):
    r = httpx.post(f"{API_URL}/chat", json={"message": message}, timeout=20)
    r.raise_for_status()
    print("B.I.H.:", r.json().get("reply", ""))


def call_paperwork(text: str):
    r = httpx.post(f"{API_URL}/paperwork/analyze", json={"text": text}, timeout=30)
    r.raise_for_status()
    data = r.json()
    print("\nB.I.H. Paperwork Helper")
    print("Summary:", data.get("plain_summary", ""))
    print("\nActions:")
    for item in data.get("key_actions", []):
        print("-", item)
    print("\nDeadlines:")
    for item in data.get("deadlines", []):
        print("-", item)
    print("\nQuestions to ask:")
    for item in data.get("questions_to_ask", []):
        print("-", item)
    print("\nNote:", data.get("safety_note", ""))


def read_multiline():
    print("Paste paperwork text. Press Enter on an empty line when done.")
    lines = []
    while True:
        line = input()
        if line.strip() == "":
            break
        lines.append(line)
    return "\n".join(lines).strip()


def main():
    print("B.I.H. CLI")
    print("Type your message for chat.")
    print("Type /paperwork for paperwork helper.")
    print("Type 'exit' to quit.")
    while True:
        msg = input("You: ").strip()
        if msg.lower() in {"exit", "quit"}:
            print("Take care.")
            break
        if msg.lower() == "/paperwork":
            text = read_multiline()
            if not text:
                print("No text provided.")
                continue
            try:
                call_paperwork(text)
            except Exception as e:
                print("Error:", e)
            continue
        try:
            call_chat(msg)
        except Exception as e:
            print("Error:", e)


if __name__ == "__main__":
    main()
