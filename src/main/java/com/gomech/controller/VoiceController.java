package com.gomech.controller;

import com.gomech.service.PythonAiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/ai/voice")
public class VoiceController {

    private final PythonAiService pythonAiService;

    public VoiceController(PythonAiService pythonAiService) {
        this.pythonAiService = pythonAiService;
    }

    /**
     * Transcrever áudio para texto (Speech-to-Text)
     */
    @PostMapping("/transcribe")
    public ResponseEntity<Map<String, Object>> transcribeAudio(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = pythonAiService.transcribeAudio(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Erro ao transcrever áudio: " + e.getMessage()
                    ));
        }
    }

    /**
     * Converter texto em áudio (Text-to-Speech)
     */
    @PostMapping("/synthesize")
    public ResponseEntity<Map<String, Object>> synthesizeSpeech(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = pythonAiService.synthesizeSpeech(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Erro ao sintetizar fala: " + e.getMessage()
                    ));
        }
    }

    /**
     * Comando de voz completo (STT + processamento + TTS opcional)
     */
    @PostMapping("/command")
    public ResponseEntity<Map<String, Object>> processVoiceCommand(@RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> response = pythonAiService.processVoiceCommand(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Erro ao processar comando de voz: " + e.getMessage()
                    ));
        }
    }
}

