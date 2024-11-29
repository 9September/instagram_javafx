CREATE DATABASE IF NOT EXISTS INSTARGRAM;
USE INSTARGRAM;

CREATE TABLE IF NOT EXISTS USERS(
	ID	VARCHAR(15) NOT NULL,
	PASSWORD VARCHAR(255) NOT NULL,
    EMAIL	VARCHAR(100) NOT NULL,
    CREATED_AT	DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    BIRTHDAY	DATE  NOT NULL,
    PHONE_NUMBER	VARCHAR(15) NOT NULL,
    profile_image LONGBLOB,
    PRIMARY KEY(ID),
    UNIQUE(EMAIL)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS POSTS (
	POST_ID INT NOT NULL AUTO_INCREMENT,
    TEXT TEXT NOT NULL,
    IMAGE LONGBLOB,
    WRITER_ID VARCHAR(15) NOT NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
	NUM_OF_LIKES INT NOT NULL DEFAULT 0,
	PRIMARY KEY(POST_ID),
    FOREIGN KEY(WRITER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    INDEX idx_writer_id (WRITER_ID)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS COMMENTS(
	COMMENT_ID INT NOT NULL AUTO_INCREMENT,
    TEXT TEXT NOT NULL,
    WRITER_ID VARCHAR(15) NOT NULL,
    POST_ID INT NOT NULL,
    NUM_OF_LIKES INT NOT NULL DEFAULT 0,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(COMMENT_ID),
    FOREIGN KEY(WRITER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    FOREIGN KEY (POST_ID) REFERENCES POSTS(POST_ID) ON DELETE CASCADE,
    INDEX idx_comment_writer_id (WRITER_ID),
    INDEX idx_comment_post_id (POST_ID)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS POST_LIKE (
	POST_ID INT NOT NULL,
    USER_ID VARCHAR(15) NOT NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(POST_ID, USER_ID),
    FOREIGN KEY(POST_ID) REFERENCES POSTS(POST_ID) ON DELETE CASCADE,
    FOREIGN KEY(USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    INDEX idx_post_like_user_id (USER_ID)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS COMMENTS_LIKE (
	COMMENT_ID INT NOT NULL,
    USER_ID VARCHAR(15) NOT NULL,
    CREATED_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(COMMENT_ID, USER_ID),
    FOREIGN KEY(COMMENT_ID) REFERENCES COMMENTS(COMMENT_ID) ON DELETE CASCADE,
    FOREIGN KEY(USER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    INDEX idx_comment_like_user_id (USER_ID)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS FOLLOW (
    FOLLOWING_ID VARCHAR(15) NOT NULL,
    FOLLOWER_ID VARCHAR(15) NOT NULL,
    CREATE_AT DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (FOLLOWING_ID, FOLLOWER_ID),
    FOREIGN KEY (FOLLOWING_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    FOREIGN KEY (FOLLOWER_ID) REFERENCES USERS(ID) ON DELETE CASCADE,
    INDEX idx_following_id (FOLLOWING_ID),
    INDEX idx_follower_id (FOLLOWER_ID),
    UNIQUE KEY unique_follow (FOLLOWING_ID, FOLLOWER_ID)
) ENGINE=InnoDB;

CREATE TABLE REELS (
    REEL_ID INT PRIMARY KEY AUTO_INCREMENT,
    VIDEO_URL VARCHAR(255) NOT NULL, -- 영상 파일의 URL 또는 경로
    DESCRIPTION VARCHAR(500), -- 영상 설명
    UPLOADER_ID VARCHAR(50) NOT NULL, -- USERS 테이블의 ID (외래 키)
    CREATED_AT TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    NUM_OF_LIKES INT DEFAULT 0,
    NUM_OF_COMMENTS INT DEFAULT 0,
    FOREIGN KEY (UPLOADER_ID) REFERENCES USERS(ID)
);

CREATE TABLE messages (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id VARCHAR(255) NOT NULL,
    receiver_id VARCHAR(255) NOT NULL,
    message_text TEXT NOT NULL,
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE messages ADD COLUMN is_read BOOLEAN DEFAULT false;
ALTER TABLE USERS ADD COLUMN is_online BOOLEAN DEFAULT FALSE;



-- Triggers for POST_LIKE
DELIMITER $$

-- Trigger to increase NUM_OF_LIKES when a like is added to a post
CREATE TRIGGER increase_post_likes AFTER INSERT ON POST_LIKE
FOR EACH ROW
BEGIN
    UPDATE POSTS
    SET NUM_OF_LIKES = NUM_OF_LIKES + 1
    WHERE POST_ID = NEW.POST_ID;
END$$

-- Trigger to decrease NUM_OF_LIKES when a like is removed from a post
CREATE TRIGGER decrease_post_likes AFTER DELETE ON POST_LIKE
FOR EACH ROW
BEGIN
    UPDATE POSTS
    SET NUM_OF_LIKES = CASE
        WHEN NUM_OF_LIKES > 0 THEN NUM_OF_LIKES - 1
        ELSE 0
    END
    WHERE POST_ID = OLD.POST_ID;
END$$

-- Triggers for COMMENTS_LIKE
-- Trigger to increase NUM_OF_LIKES when a like is added to a comment
CREATE TRIGGER increase_comment_likes AFTER INSERT ON COMMENTS_LIKE
FOR EACH ROW
BEGIN
    UPDATE COMMENTS
    SET NUM_OF_LIKES = NUM_OF_LIKES + 1
    WHERE COMMENT_ID = NEW.COMMENT_ID;
END$$

-- Trigger to decrease NUM_OF_LIKES when a like is removed from a comment
CREATE TRIGGER decrease_comment_likes AFTER DELETE ON COMMENTS_LIKE
FOR EACH ROW
BEGIN
    UPDATE COMMENTS
    SET NUM_OF_LIKES = CASE
        WHEN NUM_OF_LIKES > 0 THEN NUM_OF_LIKES - 1
        ELSE 0
    END
    WHERE COMMENT_ID = OLD.COMMENT_ID;
END$$


DELIMITER ;
