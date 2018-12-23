package com.cse2216appproject.wifitalkie;

import java.io.Serializable;
import java.net.Socket;

public class Data implements Serializable {

    public static Socket socket;
    Data(Socket socket)
    {
        this.socket=socket;
    }
}
