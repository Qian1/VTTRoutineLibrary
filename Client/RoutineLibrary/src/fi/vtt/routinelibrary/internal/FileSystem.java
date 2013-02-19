/*
 * Copyright (c) 2013, VTT Technical Research Centre of Finland 
 * All rights reserved. 
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met: 
 * 
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer. 
 * 2. Redistributions in binary form must reproduce the above copyright 
 *    notice, this list of conditions and the following disclaimer in the 
 *    documentation and/or other materials provided with the distribution. 
 * 3. Neither the name of the VTT Technical Research Centre of Finland nor the 
 *    names of its contributors may be used to endorse or promote products 
 *    derived from this software without specific prior written permission. 
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR 
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 * 
 */

package fi.vtt.routinelibrary.internal;

import android.os.Environment;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import fi.vtt.routinelibrary.common.Configuration;
import fi.vtt.routinelibrary.common.Data;
import fi.vtt.routinelibrary.common.DayData;
import fi.vtt.routinelibrary.common.RawLogData;
import fi.vtt.routinelibrary.common.RoutineData;

/**
 * File system functions. 
 *
 */

public class FileSystem {

    private final static String ROUTINELIBRARY_SAVE_DIR = "/RoutineLibraryData/";

    private static String getSavePath() {
        String savePathString = Environment.getExternalStorageDirectory().toString() + ROUTINELIBRARY_SAVE_DIR;

        return savePathString;
    }

    /**
     * Load JSON formatted data from a file. 
     * 
     * @param  fileNameStringIncoming  File name without path. 
     * @param  dataTypeIncoming  Type of object which to load from a file. 
     * 
     * @return  data  Loaded from a file. 
     * 
     * @throws  IOException
     * 
     */

    public static Data loadFromFile(String fileNameStringIncoming, Data dataTypeIncoming) throws IOException {
        String pathString = getSavePath();
        String absoluteFileNameString = pathString + fileNameStringIncoming;

        File file = new File(absoluteFileNameString);

        if (!file.exists()) {
            throw new IOException("No such file: " + absoluteFileNameString + " !");
        }

        FileInputStream fileInputStream = null;

        try {
            fileInputStream = new FileInputStream(file);

            byte[] byteArray = new byte[(int)file.length()];

            fileInputStream.read(byteArray);

            String string = new String(byteArray);

            Gson gson = new Gson();

            Data data = null;

            if (dataTypeIncoming instanceof State) {
                data = gson.fromJson(string, State.class);
            }
            else if (dataTypeIncoming instanceof RawLogData) {
                data = gson.fromJson(string, RawLogData.class);
            }
            else if (dataTypeIncoming instanceof Configuration) {
                data = gson.fromJson(string, Configuration.class);
            }
            else if (dataTypeIncoming instanceof DayData) {
                data = gson.fromJson(string, DayData.class);
            }
            else if (dataTypeIncoming instanceof RoutineData) {
                data = gson.fromJson(string, RoutineData.class);
            }
            else {
                throw new IOException("Don't know how to deserialize data type: " + dataTypeIncoming.getClass().getName() + " !");
            }

            return data;
        }
        finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }

    /**
     * Save object to a file system in JSON format. 
     * 
     * @param  fileNameStringIncoming  File name without path. 
     * @param  dataIncoming  Data to save. 
     * 
     * @throws IOException
     * 
     */

    public static void saveToFile(String fileNameStringIncoming, Data dataIncoming) throws IOException {
        String pathString = getSavePath();
        String absoluteFileNameString = pathString + fileNameStringIncoming;

        File file = new File(absoluteFileNameString);

        if (!file.exists()) {
            File directory = new File(pathString);
            directory.mkdirs();
        }

        FileOutputStream fileOutputStream = null;

        try {
            fileOutputStream = new FileOutputStream(file);

            Gson gson = new Gson();

            String json = gson.toJson(dataIncoming);

            fileOutputStream.write(json.getBytes());
        }
        finally {
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
    }

}