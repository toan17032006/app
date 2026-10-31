package backend.hoctap.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Trích xuất text từ file người dùng tải lên để đưa cho AI đọc.
 * Hỗ trợ: PDF (.pdf), Word mới (.docx), Word cũ (.doc), text (.txt, .md).
 */
@Component
@Slf4j
public class FileTextExtractor {

    /** Giới hạn kích thước file đầu vào (khớp spring.servlet.multipart.max-file-size) */
    public static final long MAX_FILE_SIZE = 15L * 1024 * 1024;

    public boolean supports(String filename) {
        String ext = extension(filename);
        return ext.equals("pdf") || ext.equals("docx") || ext.equals("doc")
                || ext.equals("txt") || ext.equals("md");
    }

    /** Đọc toàn bộ text của file — ném 400 với message tiếng Việt nếu không đọc được */
    public String extract(String filename, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File rỗng hoặc không đọc được");
        }
        if (bytes.length > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "File quá lớn (tối đa 15MB)");
        }

        String ext = extension(filename);
        try {
            String text = switch (ext) {
                case "pdf"  -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "doc"  -> extractDoc(bytes);
                default     -> new String(bytes, StandardCharsets.UTF_8);
            };

            if (text == null || text.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Không đọc được nội dung văn bản trong file. Nếu file là ảnh scan, "
                        + "hãy dùng file chữ có thể copy được.");
            }
            return text.trim();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("Extract text failed for {} : {}", filename, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không đọc được file \"" + filename + "\" (" + e.getMessage() + ")");
        }
    }

    private String extractPdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extractDoc(byte[] bytes) throws Exception {
        try (HWPFDocument doc = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(doc)) {
            return extractor.getText();
        }
    }

    private String extension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
