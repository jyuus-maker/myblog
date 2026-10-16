package com.example.myblog.config;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JasyptConfigTest {

    @Test
    void testEncryption() {
        StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("myblog-secret-key-1234");
        encryptor.setAlgorithm("PBEWithMD5AndDES");

        String plainText = "My123456789!";
        String encryptedText = encryptor.encrypt(plainText);
        System.out.println("ENCRYPTED_PASSWORD: ENC(" + encryptedText + ")");

        String decryptedText = encryptor.decrypt(encryptedText);
        assertThat(decryptedText).isEqualTo(plainText);
    }
}
