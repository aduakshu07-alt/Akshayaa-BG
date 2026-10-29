
import java.sql.*;
import java.util.Scanner;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public class OnlineCourseSystem {

    static final String URL =
        "jdbc:mysql://localhost:3306/online_course_db";
    static final String USER = "root";
    static final String PASSWORD = "root123";

    static Connection con;
    static Scanner sc = new Scanner(System.in);
    static int studentId = -1;

    public static void main(String[] args) {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASSWORD);

            System.out.println("================================");
            System.out.println(" ONLINE COURSE RESERVATION");
            System.out.println("================================");

            while (true) {
                System.out.println("\n1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Enter choice: ");

                int choice = readInt();

                switch (choice) {
                    case 1:
                        register();
                        break;
                    case 2:
                        if (login()) {
                            studentMenu();
                        }
                        break;
                    case 3:
                        con.close();
                        System.out.println("Thank you!");
                        return;
                    default:
                        System.out.println("Invalid choice.");
                }
            }
        } catch (Exception e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }

    static int readInt() {
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static String hashPassword(String password)
            throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] bytes = md.digest(
            password.getBytes(StandardCharsets.UTF_8));

        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b & 0xff));
        }
        return result.toString();
    }

    static void register() throws Exception {
        System.out.print("Enter name: ");
        String name = sc.nextLine().trim();

        System.out.print("Enter email: ");
        String email = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        if (name.isEmpty() || email.isEmpty()
                || password.isEmpty()) {
            System.out.println("All fields are required.");
            return;
        }

        String sql = "INSERT INTO students "
                   + "(name, email, password_hash) "
                   + "VALUES (?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, hashPassword(password));
            ps.executeUpdate();

            System.out.println("Registration successful!");
        } catch (SQLIntegrityConstraintViolationException e) {
            System.out.println("Email already registered.");
        }
    }

    static boolean login() throws Exception {
        System.out.print("Enter email: ");
        String email = sc.nextLine().trim();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        String sql = "SELECT student_id FROM students "
                   + "WHERE email = ? AND password_hash = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, hashPassword(password));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    studentId = rs.getInt("student_id");
                    System.out.println("Login successful!");
                    return true;
                }
            }
        }

        System.out.println("Invalid email or password.");
        return false;
    }

    static void studentMenu() throws Exception {
        while (true) {
            System.out.println("\n===== STUDENT DASHBOARD =====");
            System.out.println("1. Browse courses");
            System.out.println("2. Register and pay for a course");
            System.out.println("3. View my courses and progress");
            System.out.println("4. Update course progress");
            System.out.println("5. Logout");
            System.out.print("Enter choice: ");

            int choice = readInt();

            switch (choice) {
                case 1:
                    browseCourses();
                    break;
                case 2:
                    enrollCourse();
                    break;
                case 3:
                    viewMyCourses();
                    break;
                case 4:
                    updateProgress();
                    break;
                case 5:
                    studentId = -1;
                    return;
                default:
                    System.out.println("Invalid choice.");
            }
        }
    }

    static void browseCourses() throws Exception {
        String sql = "SELECT * FROM courses ORDER BY course_id";

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            System.out.println("\n===== AVAILABLE COURSES =====");

            while (rs.next()) {
                System.out.println("\nCourse ID: "
                    + rs.getInt("course_id"));
                System.out.println("Name: "
                    + rs.getString("course_name"));
                System.out.println("Instructor: "
                    + rs.getString("instructor"));
                System.out.println("Description: "
                    + rs.getString("description"));
                System.out.println("Fee: Rs. "
                    + rs.getBigDecimal("fee"));
            }
        }
    }

    static void enrollCourse() throws Exception {
        browseCourses();

        System.out.print("\nEnter course ID to enroll: ");
        int courseId = readInt();

        String sql = "SELECT course_name, fee FROM courses "
                   + "WHERE course_id = ?";

        String courseName;
        java.math.BigDecimal fee;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, courseId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Course not found.");
                    return;
                }

                courseName = rs.getString("course_name");
                fee = rs.getBigDecimal("fee");
            }
        }

        // Check whether the student is already enrolled.
        sql = "SELECT enrollment_id FROM enrollments "
            + "WHERE student_id = ? AND course_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, courseId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("Already enrolled.");
                    return;
                }
            }
        }

        System.out.println("Course: " + courseName);
        System.out.println("Amount: Rs. " + fee);
        System.out.println("1. Confirm demo payment");
        System.out.println("2. Cancel");
        System.out.print("Enter choice: ");

        if (readInt() != 1) {
            System.out.println("Enrollment cancelled.");
            return;
        }

        // Save enrollment and payment together.
        con.setAutoCommit(false);

        try {
            sql = "INSERT INTO enrollments "
                + "(student_id, course_id, progress) "
                + "VALUES (?, ?, 0)";

            int enrollmentId;

            try (PreparedStatement ps = con.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, studentId);
                ps.setInt(2, courseId);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException(
                            "Could not create enrollment.");
                    }
                    enrollmentId = keys.getInt(1);
                }
            }

            sql = "INSERT INTO payments "
                + "(enrollment_id, amount, payment_status) "
                + "VALUES (?, ?, 'PAID')";

            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setInt(1, enrollmentId);
                ps.setBigDecimal(2, fee);
                ps.executeUpdate();
            }

            con.commit();

            System.out.println("Enrollment successful!");
            System.out.println("Demo payment recorded.");
            System.out.println("Course: " + courseName);
            System.out.println("Status: PAID (simulation)");

        } catch (Exception e) {
            con.rollback();
            System.out.println("Enrollment failed: "
                + e.getMessage());
        } finally {
            con.setAutoCommit(true);
        }
    }

    static void viewMyCourses() throws Exception {
        String sql =
            "SELECT e.enrollment_id, c.course_name, "
          + "c.instructor, e.progress, p.payment_status "
          + "FROM enrollments e "
          + "JOIN courses c ON e.course_id = c.course_id "
          + "JOIN payments p ON e.enrollment_id = p.enrollment_id "
          + "WHERE e.student_id = ? ORDER BY e.enrollment_id";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n===== MY COURSES =====");

                boolean found = false;

                while (rs.next()) {
                    found = true;
                    System.out.println("\nEnrollment ID: "
                        + rs.getInt("enrollment_id"));
                    System.out.println("Course: "
                        + rs.getString("course_name"));
                    System.out.println("Instructor: "
                        + rs.getString("instructor"));
                    System.out.println("Progress: "
                        + rs.getInt("progress") + "%");
                    System.out.println("Payment: "
                        + rs.getString("payment_status"));
                }

                if (!found) {
                    System.out.println("No enrolled courses.");
                }
            }
        }
    }

    static void updateProgress() throws Exception {
        viewMyCourses();

        System.out.print("\nEnter enrollment ID: ");
        int enrollmentId = readInt();

        System.out.print("Enter progress (0-100): ");
        int progress = readInt();

        if (progress < 0 || progress > 100) {
            System.out.println("Progress must be 0 to 100.");
            return;
        }

        String sql = "UPDATE enrollments SET progress = ? "
                   + "WHERE enrollment_id = ? AND student_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, progress);
            ps.setInt(2, enrollmentId);
            ps.setInt(3, studentId);

            if (ps.executeUpdate() > 0) {
                System.out.println("Progress updated to "
                    + progress + "%.");
                if (progress == 100) {
                    System.out.println("Course completed!");
                }
            } else {
                System.out.println("Invalid enrollment ID.");
            }
        }
    }
}
