package com.ids.expense.service;

import com.ids.expense.common.models.*;
import com.ids.expense.common.repository.ExpenseAttachmentRepository;
import com.ids.expense.common.repository.ExpenseLineRepository;
import com.ids.expense.common.repository.ExpenseReportRepository;
import com.ids.expense.common.repository.UserRepository;
import com.ids.expense.config.FileStorageProperties;
import com.ids.expense.note_de_frais.response.ExpenseAttachmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".jpg", ".jpeg", ".png");

    private final ExpenseAttachmentRepository attachmentRepository;
    private final ExpenseReportRepository reportRepository;
    private final ExpenseLineRepository lineRepository;
    private final UserRepository userRepository;
    private final FileStorageProperties storageProperties;

    @Transactional
    public ExpenseAttachmentResponse upload(Long reportId, Long lineId, MultipartFile file, Long userId) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note de frais introuvable"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        assertOwner(user, report);

        if (report.getStatus() != ExpenseStatus.DRAFT && report.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("Impossible d'ajouter un justificatif sur une note déjà soumise");
        }

        validateFile(file);

        ExpenseLine line = null;
        if (lineId != null) {
            line = lineRepository.findById(lineId)
                    .orElseThrow(() -> new RuntimeException("Ligne de dépense introuvable"));
            if (!line.getReport().getId().equals(reportId)) {
                throw new RuntimeException("La ligne n'appartient pas à cette note");
            }
        }

        String storedName = storeFile(file);

        ExpenseAttachment attachment = new ExpenseAttachment();
        attachment.setReport(report);
        attachment.setLine(line);
        attachment.setOriginalFileName(sanitizeFileName(file.getOriginalFilename()));
        attachment.setStoredFileName(storedName);
        attachment.setContentType(resolveContentType(file));
        attachment.setFileSize(file.getSize());
        attachment.setUploadedAt(LocalDateTime.now());

        ExpenseAttachment saved = attachmentRepository.save(attachment);
        return mapToResponse(saved);
    }

    /** Usage interne : les contrôles d'accès sont faits en amont par ExpenseService. */
    public List<ExpenseAttachmentResponse> listByReportIdInternal(Long reportId) {
        return attachmentRepository.findByReportId(reportId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseAttachmentResponse> listByLineIdInternal(Long lineId) {
        return attachmentRepository.findByLineId(lineId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<ExpenseAttachmentResponse> listByReport(Long reportId, Long userId) {
        ExpenseReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Note de frais introuvable"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        assertCanView(user, report);

        return attachmentRepository.findByReportId(reportId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ExpenseAttachmentResponse getById(Long attachmentId, Long userId) {
        ExpenseAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Justificatif introuvable"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        assertCanView(user, attachment.getReport());
        return mapToResponse(attachment);
    }

    public DownloadPayload loadForDownload(Long attachmentId, Long userId) {
        ExpenseAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Justificatif introuvable"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        assertCanView(user, attachment.getReport());

        try {
            Path path = getUploadPath().resolve(attachment.getStoredFileName());
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists()) {
                throw new RuntimeException("Fichier introuvable sur le disque");
            }
            return new DownloadPayload(resource, attachment.getOriginalFileName(), attachment.getContentType());
        } catch (Exception e) {
            throw new RuntimeException("Impossible de lire le justificatif", e);
        }
    }

    @Transactional
    public void delete(Long attachmentId, Long userId) {
        ExpenseAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Justificatif introuvable"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        assertOwner(user, attachment.getReport());

        ExpenseReport report = attachment.getReport();
        if (report.getStatus() != ExpenseStatus.DRAFT && report.getStatus() != ExpenseStatus.REJECTED) {
            throw new RuntimeException("Impossible de supprimer un justificatif d'une note soumise");
        }

        deleteFromDisk(attachment);
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void deleteByLineId(Long lineId) {
        for (ExpenseAttachment attachment : attachmentRepository.findByLineId(lineId)) {
            deleteFromDisk(attachment);
            attachmentRepository.delete(attachment);
        }
    }

    private void deleteFromDisk(ExpenseAttachment attachment) {
        try {
            Files.deleteIfExists(getUploadPath().resolve(attachment.getStoredFileName()));
        } catch (IOException ignored) {
        }
    }

    private void assertOwner(User user, ExpenseReport report) {
        if (!report.getEmployee().getId().equals(user.getId())) {
            throw new RuntimeException("Vous ne pouvez modifier que vos propres notes de frais");
        }
    }

    private void assertCanView(User user, ExpenseReport report) {
        if (report.getEmployee().getId().equals(user.getId())) {
            return;
        }
        RoleType role = user.getRole();
        if (role == RoleType.ADMIN) {
            return;
        }
        if (report.getStatus() == ExpenseStatus.IN_PROGRESS && report.getCurrentStep() != null) {
            RoleType required = report.getCurrentStep().getRequiredRole();
            if (user.getRole() == required) {
                if (role == RoleType.MANAGER || role == RoleType.TECHNICAL_DIRECTOR) {
                    if (user.getDepartment() != null && report.getEmployee().getDepartment() != null
                            && user.getDepartment().getId().equals(report.getEmployee().getDepartment().getId())) {
                        return;
                    }
                } else {
                    return;
                }
            }
        }
        if (role == RoleType.ACCOUNTANT
                && (report.getStatus() == ExpenseStatus.APPROVED || report.getStatus() == ExpenseStatus.PAID)) {
            return;
        }
        if (role == RoleType.GENERAL_DIRECTOR || role == RoleType.TECHNICAL_DIRECTOR) {
            return;
        }
        throw new RuntimeException("Accès non autorisé à ce justificatif");
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Le fichier est vide");
        }
        if (file.getSize() > storageProperties.getMaxFileSizeBytes()) {
            throw new RuntimeException("Fichier trop volumineux (maximum 10 Mo)");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Extension non autorisée. Formats acceptés : PDF, JPG, JPEG, PNG");
        }

        String contentType = file.getContentType();
        if (contentType != null
                && !contentType.equals("application/octet-stream")
                && !storageProperties.getAllowedContentTypes().contains(contentType)) {
            throw new RuntimeException("Type de fichier non autorisé. Formats acceptés : PDF, JPEG, PNG");
        }
    }

    private String resolveContentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("application/octet-stream")) {
            return contentType;
        }
        return switch (extractExtension(file.getOriginalFilename())) {
            case ".pdf" -> "application/pdf";
            case ".png" -> "image/png";
            default -> "image/jpeg";
        };
    }

    private String sanitizeFileName(String filename) {
        if (filename == null || filename.isBlank()) {
            return "justificatif";
        }
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    public static String encodeFilename(String filename) {
        return URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String storeFile(MultipartFile file) {
        try {
            Path uploadPath = getUploadPath();
            Files.createDirectories(uploadPath);
            String extension = extractExtension(file.getOriginalFilename());
            String storedName = UUID.randomUUID() + extension;
            Files.copy(file.getInputStream(), uploadPath.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
            return storedName;
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de l'enregistrement du fichier", e);
        }
    }

    private Path getUploadPath() {
        return Paths.get(storageProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }

    private ExpenseAttachmentResponse mapToResponse(ExpenseAttachment attachment) {
        ExpenseAttachmentResponse response = new ExpenseAttachmentResponse();
        response.setId(attachment.getId());
        response.setReportId(attachment.getReport().getId());
        response.setLineId(attachment.getLine() != null ? attachment.getLine().getId() : null);
        response.setOriginalFileName(attachment.getOriginalFileName());
        response.setContentType(attachment.getContentType());
        response.setFileSize(attachment.getFileSize());
        response.setUploadedAt(attachment.getUploadedAt());
        response.setDownloadUrl("/api/expenses/attachments/" + attachment.getId() + "/download");
        return response;
    }

    public record DownloadPayload(Resource resource, String originalFileName, String contentType) {}
}
