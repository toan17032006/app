"""
Mock server OpenAI-compatible (dùng để test tính năng AI của EduSmart không cần API key).
Chạy:  python samples/mock-ai-server.py   (port 11500)
Backend: AI_BASE_URL=http://localhost:11500/v1 AI_API_KEY=mock AI_MODEL=mock
"""
import http.server
import json

EXAM_JSON = {
    "title": "De thi OOP Java - AI tao tu tai lieu",
    "category": "Lap trinh Java",
    "durationMinutes": 90,
    "questions": [
        {"questionText": "OOP la gi?", "options": ["Phuong phap lap trinh dua tren doi tuong",
            "Ngon ngu lap trinh", "He quan tri co so du lieu", "Thuat toan sap xep"],
            "correctOptionIndex": 0,
            "explanation": "Tai lieu muc 1: OOP la phuong phap lap trinh dua tren khai niem doi tuong."},
        {"questionText": "Dac trung nao giu thong tin chi tiet ben trong doi tuong?",
            "options": ["Ke thua", "Dong goi", "Da hinh", "Truu tuong"],
            "correctOptionIndex": 1,
            "explanation": "Tai lieu muc 2: Dong goi (Encapsulation) giau thong tin chi tiet ben trong."},
        {"questionText": "Java cho phep ke thua bao nhieu lop cha?",
            "options": ["2", "3", "1 (ke thua don)", "Khong gioi han"],
            "correctOptionIndex": 2,
            "explanation": "Tai lieu: Java chi cho ke thua don bang tu khoa extends."},
        {"questionText": "Tu khoa nao dung de tao object tu class?",
            "options": ["create", "instanceof", "new", "static"],
            "correctOptionIndex": 2,
            "explanation": "Tai lieu muc 3: object duoc tao tu class bang tu khoa new."},
        {"questionText": "Interface truoc Java 8 chi chua gi?",
            "options": ["Thuoc tinh private", "Khai bao phuong thuc", "Constructor", "Block static"],
            "correctOptionIndex": 1,
            "explanation": "Tai lieu muc 2: Interface chi chua khai bao phuong thuc truoc Java 8."},
    ],
}

HOMEWORK_JSON = dict(EXAM_JSON)
HOMEWORK_JSON["title"] = "Bai tap luyen tap OOP - AI tao tu tai lieu"
HOMEWORK_JSON["durationMinutes"] = 0


class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length).decode("utf-8", errors="replace")
        # Chế độ HOMEWORK trong prompt yêu cầu giải thích bắt buộc → trả bản có giải thích + 0 phút
        mode = "HOMEWORK" if "BÀI TẬP" in raw.upper() or "KHÔNG GIỚI HẠN" in raw.upper() else "EXAM"
        payload = HOMEWORK_JSON if mode == "HOMEWORK" else EXAM_JSON
        resp = {"id": "mock-1", "object": "chat.completion", "model": "mock",
                "choices": [{"index": 0,
                             "message": {"role": "assistant",
                                         "content": json.dumps(payload, ensure_ascii=False)},
                             "finish_reason": "stop"}]}
        data = json.dumps(resp, ensure_ascii=False).encode("utf-8")
        print(f"MOCK-AI {self.path} mode={mode} bytes={length}", flush=True)
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    print("Mock AI server listening on http://127.0.0.1:11500/v1", flush=True)
    http.server.HTTPServer(("127.0.0.1", 11500), Handler).serve_forever()
