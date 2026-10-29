
CREATE DATABASE IF NOT EXISTS online_course_db;
USE online_course_db;

CREATE TABLE students (
    student_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password_hash VARCHAR(64) NOT NULL
);

CREATE TABLE courses (
    course_id INT AUTO_INCREMENT PRIMARY KEY,
    course_name VARCHAR(150) NOT NULL,
    instructor VARCHAR(100),
    description VARCHAR(255),
    fee DECIMAL(10,2) NOT NULL
);

CREATE TABLE enrollments (
    enrollment_id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    course_id INT NOT NULL,
    progress INT DEFAULT 0,
    enrollment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES students(student_id),
    FOREIGN KEY (course_id) REFERENCES courses(course_id),
    CHECK (progress BETWEEN 0 AND 100)
);

CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id INT NOT NULL UNIQUE,
    amount DECIMAL(10,2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT 'PAID',
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id)
        REFERENCES enrollments(enrollment_id)
);

INSERT INTO courses
(course_name, instructor, description, fee)
VALUES
('Java Programming', 'Arun Kumar',
 'Learn Java and OOP', 999.00),
('Python Programming', 'Priya Sharma',
 'Learn Python basics', 799.00),
('Web Development', 'Rahul Kumar',
 'Learn HTML, CSS and JavaScript', 1199.00),
('Cybersecurity Basics', 'Vijay Anand',
 'Learn network security basics', 1499.00);
