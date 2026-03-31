package org.example.service;

import io.minio.MinioClient;
import org.example.config.MinioConfig;
import org.example.repository.BookingRepository;
import org.example.repository.DocumentRepository;
import org.example.repository.PropertyRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.example.model.User;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentServiceValidationTest {

    private MinioClient minioClient;
    private MinioConfig.MinioProperties props;
    private DocumentRepository documentRepository;
    private BookingRepository bookingRepository;
    private PropertyRepository propertyRepository;
    private UserRepository userRepository;

    private DocumentService service;

    @BeforeEach
    void setup() {
        minioClient = mock(MinioClient.class);
        props = new MinioConfig.MinioProperties();
        props.setBucket("test");
        props.setEndpoint("http://localhost");
        props.setAccessKey("x");
        props.setSecretKey("y");

        documentRepository = mock(DocumentRepository.class);
        bookingRepository = mock(BookingRepository.class);
        propertyRepository = mock(PropertyRepository.class);
        userRepository = mock(UserRepository.class);

        service = new DocumentService(minioClient, props, documentRepository, bookingRepository, propertyRepository, userRepository);
    }

    @Test
    void upload_rejectsUnsupportedMimeType() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).email("u@test").build()));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "x.txt",
                "text/plain",
                "hello".getBytes()
        );

        assertThatThrownBy(() ->
                service.uploadDocument(1L, "Doc", "CONTRACT", null, null, file)
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void upload_rejectsTooLargeFile() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).email("u@test").build()));
        byte[] bytes = new byte[11 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "x.pdf",
                "application/pdf",
                bytes
        );

        assertThatThrownBy(() ->
                service.uploadDocument(1L, "Doc", "CONTRACT", null, null, file)
        ).isInstanceOf(IllegalArgumentException.class)
         .hasMessageContaining("File size");
    }
}

