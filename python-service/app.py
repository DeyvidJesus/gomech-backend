from flask import Flask, jsonify

app = Flask(__name__)

@app.get("/health")
def health():
    return jsonify(status="ok"), 200

@app.get("/api/data")
def data():
    return jsonify(message="Hello from lightweight Flask service!"), 200

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
