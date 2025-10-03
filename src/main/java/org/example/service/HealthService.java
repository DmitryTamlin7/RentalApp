package org.example.service;


import lombok.AllArgsConstructor;
import lombok.Lombok;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor

@Service
public class HealthService {
    private  static  final DateTimeFormatter format =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public HealthResponse getHealthStatus() {
        return new HealthResponse(
                "OK",
                LocalDateTime.now().format(format)
        );
    }

    public static class HealthResponse {
        private String status;
        private String timestamp;

        public HealthResponse(String status, String timestamp) {
            this.status = status;
            this.timestamp = timestamp;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(String timestamp) {
            this.timestamp = timestamp;
        }
    }

}
