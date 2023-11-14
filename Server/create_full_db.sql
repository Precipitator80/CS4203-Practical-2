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
    rsa_public_key VARBINARY(294),
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
CREATE OR REPLACE PROCEDURE proc_read_chat(IN chat_id INT, IN offset_val INT) BEGIN
SELECT line_text
FROM chat_line
WHERE chat_id = chat_line.chat_id
ORDER BY chat_line.id DESC
LIMIT 25 OFFSET offset_val;
END;
/*----------------------------------------------*/
-- Procedure to create a new chat, returning the ID.
CREATE OR REPLACE PROCEDURE proc_create_chat(
        IN chat_name VARCHAR(256),
        IN rsa_public_key VARBINARY(294),
        OUT id INT
    ) BEGIN
INSERT INTO chat (chat_name, rsa_public_key)
VALUES (chat_name, rsa_public_key);
SELECT LAST_INSERT_ID() INTO id;
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
        "AHwOhgykSMPUflEXtauY5w==",
        UNHEX(
            "30820122300D06092A864886F70D01010105000382010F003082010A0282010100C49F7019B0CDCF003136001004818D181C0644FA55D5F77B2AE65A45884A5AB058E77D52F788BB7DF05646FAF55A4E616CD222B7624B012191BA81C9EC8EB0E089E088A1FC088DCD2BC0E66E7608CE579B3EC1A7E9DA6B9F85E29BB5F3B33BA6069D5AB1C33910562871A3FB2B56A4C2FF707C178521C7FF6529A763B3177D3F6FD95E8A241AEF56908DC73CDD9E0DF98CFCE3991B8A745B8887DF2D71ADFAE19EDE61D899F44526968035E6308DBB23907D0905A320B2FD0D125BE74C6E013D2C2701050BDD5FEF456BA78641E52DAF0F153928A29EC9CB276B242D62EA77429994F42F14F3A170083A97B66B6525AB7624A946B29594FA9EDCCA58B27C46530203010001"
        )
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