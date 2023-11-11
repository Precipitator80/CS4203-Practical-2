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
    chat_name VARCHAR(100),
    chat_password VARCHAR(64),
    PRIMARY KEY (id)
);
CREATE TABLE chat_line (
    id INT AUTO_INCREMENT,
    chat_id INT,
    line_text TEXT,
    PRIMARY KEY (id),
    FOREIGN KEY (chat_id) REFERENCES chat (id) ON DELETE CASCADE ON UPDATE CASCADE
);
/*------------------------------------------------------------------------------------------------*/
-- Create functions
/*----------------------------------------------*/
-- Read messages from a chat if authorised.
CREATE OR REPLACE FUNCTION func_read_chat(chat_id INT, chat_password VARCHAR(64)) RETURNS TEXT BEGIN
DECLARE chat_message TEXT;
CALL proc_read_chat(chat_id, chat_password, chat_message);
RETURN chat_message;
END;
/*----------------------------------------------*/
-- Check whether an ID and password match a chat.
CREATE OR REPLACE FUNCTION func_valid_chat_credentials(chat_id INT, chat_password VARCHAR(64)) RETURNS BOOLEAN BEGIN RETURN(
        EXISTS(
            SELECT *
            FROM chat
            WHERE chat_id = chat.id
                AND chat_password = chat.chat_password
        )
    );
END;
/*------------------------------------------------------------------------------------------------*/
-- Create procedures
/*----------------------------------------------*/
-- Create a new chat.
CREATE OR REPLACE PROCEDURE proc_create_chat(
        IN chat_name VARCHAR(100),
        IN chat_password VARCHAR(64)
    ) BEGIN
INSERT INTO chat (chat_name, chat_password)
VALUES(chat_name, chat_password);
END;
/*----------------------------------------------*/
-- Write a new chat line (message).
CREATE OR REPLACE PROCEDURE proc_create_chat_line(IN chat_id INT, IN line_text TEXT) BEGIN
INSERT INTO chat_line (chat_id, line_text)
VALUES(chat_id, line_text);
END;
/*----------------------------------------------*/
-- Only allow reading a chat if the user is authorised.
CREATE OR REPLACE PROCEDURE proc_read_chat(
        IN chat_id INT,
        IN chat_password VARCHAR(64),
        OUT chat_message TEXT
    ) BEGIN IF func_valid_chat_credentials(chat_id, chat_password) THEN
SELECT max(line_text)
FROM chat_line
WHERE chat_id = chat_line.chat_id INTO chat_message;
ELSE
SELECT "Could not read chat. Error when checking credentials." INTO chat_message;
END IF;
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
CALL proc_create_chat(
    "The first chat!",
    "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
);
CALL proc_create_chat_line(1, "This is the first message!");