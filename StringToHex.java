import java.util.Base64;

public class StringToHex {
    public static void main(String[] args) {
        String base64String = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxJ9wGbDNzwAxNgAQBIGNGBwGRPpV1fd7KuZaRYhKWrBY531S94i7ffBWRvr1Wk5hbNIit2JLASGRuoHJ7I6w4IngiKH8CI3NK8DmbnYIzlebPsGn6dprn4Xim7XzszumBp1ascM5EFYocaP7K1akwv9wfBeFIcf/ZSmnY7MXfT9v2V6KJBrvVpCNxzzdng35jPzjmRuKdFuIh98tca364Z7eYdiZ9EUmloA15jCNuyOQfQkFoyCy/Q0SW+dMbgE9LCcBBQvdX+9Fa6eGQeUtrw8VOSiinsnLJ2skLWLqd0KZlPQvFPOhcAg6l7ZrZSWrdiSpRrKVlPqe3MpYsnxGUwIDAQAB";

        // Decode Base64 string to bytes
        byte[] binaryRepresentation = Base64.getDecoder().decode(base64String);

        // Convert binary representation to hexadecimal string
        StringBuilder hexStringBuilder = new StringBuilder();
        for (byte b : binaryRepresentation) {
            hexStringBuilder.append(String.format("%02X", b));
        }

        // Print the hexadecimal representation
        System.out.println(hexStringBuilder.toString());
    }
}