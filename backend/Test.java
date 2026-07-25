public class Test {
    public static void main(String[] args) {
        org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        System.out.println(encoder.matches("password", "$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCGZEGnd3p0eN3F8O4hJq"));
    }
}
