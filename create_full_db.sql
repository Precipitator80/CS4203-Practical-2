/*------------------------------------------------------------------------------------------------*/
-- Clean up the database.
/*----------------------------------------------*/
DROP TABLE IF EXISTS chat_line;
DROP TABLE IF EXISTS chat;
/*------------------------------------------------------------------------------------------------*/
-- Create tables. Schema adapted from the following source:
-- DB Schema For Chats?
-- Peter Bailey
-- https://stackoverflow.com/questions/3094495/db-schema-for-chats
-- Accessed 09.11.2023
/*----------------------------------------------*/
CREATE TABLE chat (
    id INT AUTO_INCREMENT,
    chat_name VARCHAR(256),
    rsa_public_key VARCHAR(256),
    PRIMARY KEY (id)
);
CREATE TABLE chat_line (
    id INT AUTO_INCREMENT,
    chat_id INT,
    line_text VARCHAR(2048),
    PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES chat (id) ON DELETE CASCADE ON UPDATE CASCADE
);
/*------------------------------------------------------------------------------------------------*/
-- Create functions
/*----------------------------------------------*/
/*------------------------------------------------------------------------------------------------*/
-- Create procedures
/*----------------------------------------------*/
-- Only allow reading a chat if the user is authorised.
CREATE OR REPLACE PROCEDURE proc_read_chat(IN chat_id INT) BEGIN
SELECT line_text
FROM chat_line
WHERE chat_id = chat_line.chat_id
ORDER BY chat_line.chat_id DESC
LIMIT 10;
END;
/*------------------------------------------------------------------------------------------------*/
-- Create views
/*----------------------------------------------*/
/*------------------------------------------------------------------------------------------------*/
-- Create triggers
/*----------------------------------------------*/
/*------------------------------------------------------------------------------------------------*/
-- Insert values
/*----------------------------------------------*/
INSERT INTO chat (chat_name, rsa_public_key)
VALUES (
        "The first chat!",
        "ThisShouldBeThePublicKey"
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "J8bvFpNReP1sB9vhwN7e2Q65CHEwfMDaaIUXdAr6GKg="
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "HqrvZALqOj3w71nbtcp3kvJm7sNGvjtXQbaNDMl8Dk0="
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "gq2/kGAY8NkkBgWOfWcKPQol19Y6GDFuSU6luikN+tA="
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "8eMEY+AsIaAxD2X0JbiTkzYaN83VLLng4ryLrzEcZhI="
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "Gz4Z45yX6weio7Ol68pKvk99Qu3CJrp9UGS5yMyIEfU="
    );
INSERT INTO chat_line (chat_id, line_text)
VALUES(
        1,
        "clBhG9jsOMxAu1Bzp2+s4099Qu3CJrp9UGS5yMyIEfU="
    );