from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="BIH API", version="0.1.0")


class ChatRequest(BaseModel):
    message: str


class ChatResponse(BaseModel):
    reply: str


@app.get("/health")
def health():
    return {"status": "ok"}


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest):
    # Placeholder until an LLM provider is wired in.
    return ChatResponse(reply=f"I hear you. You said: {req.message}")
