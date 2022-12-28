/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package parking;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author sg
 */
public class Commands {

    static String fileName;
    static String idxName;
    static String fileNameBak;
    static String idxNameBak;
    static String path;
    
    static final String IDX_EXT = "idx";
    static String fileExt;
    
    static public synchronized void setFile(File file) {
        fileName = file.getName();
        path = file.getPath();
        path = path.substring(0, path.indexOf(fileName));
        String [] str = fileName.split("\\.");
        fileExt = str[1];
        idxName = str[0] + "." + IDX_EXT;
        fileNameBak = str[0] + ".~" + fileExt;
        idxNameBak = str[0] + ".~" + IDX_EXT;

        fileName = path + fileName;
        idxName = path + idxName;
        fileNameBak = path + fileNameBak;
        idxNameBak = path + idxNameBak;
    }
    
    private static synchronized void deleteBackup() {
        new File( fileNameBak ).delete();
        new File( idxNameBak ).delete();				
    }
	
    private static synchronized void backup() {
        deleteBackup();
        new File( fileName ).renameTo( new File( fileNameBak ));
        new File( idxName ).renameTo( new File( idxNameBak ));
    }
    
    public static void fileCopy(String from, String to) throws IOException {
        byte [] buf = new byte[8192];
        int size;
        try( FileInputStream fis = new FileInputStream(from);
             FileOutputStream fos = new FileOutputStream(to)) {
            while ((size = fis.read(buf)) > 0) {
                fos.write(buf, 0, size);
            }
        }
    }

    private static synchronized void backupCopy() throws IOException {
        try {
            backup();
        }
        catch (Error | Exception ex) {}
        fileCopy(fileNameBak, fileName);
        fileCopy(idxNameBak, idxName);
    }
    
    public static synchronized void appendFile(boolean zipped, Car car)
            throws FileNotFoundException, IOException, ClassNotFoundException,
            KeyNotUniqueException {
        try {
            backupCopy();
        }
        catch (FileNotFoundException ex) {
        }
        try ( Index idx = Index.load(idxName);  
              RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            if (car == null) {
                return;
            }
            idx.test(car);
            long pos = Buffer.writeObject(raf, car, zipped);
            idx.put(car, pos);
        }
    }
    
    public static synchronized List<String> readFile() 
            throws FileNotFoundException, IOException, ClassNotFoundException {
        ArrayList<String> list = new ArrayList<>();
        long pos;
        try ( RandomAccessFile raf = new RandomAccessFile( fileName, "rw" )) {
            while (( pos = raf.getFilePointer()) < raf.length() ) {
                Car car = (Car) Buffer.readObject( raf, pos, null );
                list.add(car.toString());
            }
            System.out.flush();
        }	
        return list;
    }

    private static IndexBase indexByArg( String arg, Index idx ) 
            throws IllegalArgumentException {
        switch (arg) {
            case "Serial Numbers":
                return idx.serialNums;
            case "Owners":
                return idx.owners;
            case "Times":
                return idx.times;
            default:
                throw new IllegalArgumentException("Illegal argument: " + arg);
        }
    }
	
    public static synchronized List<String> readFile(String arg, boolean reverse) 
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>();
        try ( Index idx = Index.load(idxName); 
              RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
            IndexBase pidx = indexByArg( arg, idx );
            String[] keys = 
                pidx.getKeys(reverse ? new KeyCompReverse() : new KeyComp());
            for ( String key : keys ) {
                Long[] poss = pidx.get( key );
                for ( long pos : poss ) {
                    Car car = (Car) Buffer.readObject( raf, pos, null );
                    if ( arg.equals("Owner")) {
                        car.setOwner(key);
                    }
                    list.add(car.toString());
                }		
            }
        }
        return list;
    }
    
    public static synchronized List<String> findByKey(String type, String value) 
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>(); 
        try ( Index idx = Index.load( idxName ); 
              RandomAccessFile raf = new RandomAccessFile( fileName, "rw" )) {
            IndexBase pidx = indexByArg( type, idx );
            if (pidx.contains(value)== false) {
                throw new IOException("Key not found: " + value);	
            }
            Long[] poss = pidx.get(value);
            for ( long pos : poss ) {
                Car car = (Car) Buffer.readObject( raf, pos, null );
                list.add(car.toString());
            }		
        }
        return list;	
    }    

    public static synchronized List<String> findByKey(String type, String value, int cmp) 
            throws ClassNotFoundException, IOException, IllegalArgumentException {
        ArrayList<String> list = new ArrayList<>(); 
        try ( Index idx = Index.load( idxName ); 
              RandomAccessFile raf = new RandomAccessFile( fileName, "rw" )) {
            IndexBase pidx = indexByArg( type, idx );
            if ( pidx.contains(value)== false ) {
                throw new IOException("Key not found: " + value);	
            }
            Comparator<String> comp = (cmp == 2) ? new KeyComp(): new KeyCompReverse();
            String[] keys = pidx.getKeys(comp);
            for (String key : keys) {
                if (!key.equals(value)) {
                    Long[] poss = pidx.get(key);
                    for ( long pos : poss ) {
                        Car car = (Car) Buffer.readObject( raf, pos, null );
                        list.add(car.toString());
                    }	
                }
            }
        }
        return list;	
    }    
    
    public static synchronized void deleteFile(String type, String value)
            throws ClassNotFoundException, IOException, KeyNotUniqueException, 
            IllegalArgumentException {
        Long[] poss;
        try ( Index idx = Index.load(idxName)) {
            IndexBase pidx = indexByArg(type, idx);
            if (pidx.contains(value) == false) {
                throw new IOException("Key not found: " + value);
            }
            poss = pidx.get(value);
        }
        backup();
        Arrays.sort(poss);
        try ( Index idx = Index.load(idxName);  
              RandomAccessFile fileBak = new RandomAccessFile(fileNameBak, "rw");  
              RandomAccessFile file = new RandomAccessFile(fileName, "rw")) {
            boolean[] wasZipped = new boolean[]{false};
            long pos;
            while ((pos = fileBak.getFilePointer()) < fileBak.length()) {
                Car car = (Car) Buffer.readObject(fileBak, pos, wasZipped);
                if (Arrays.binarySearch(poss, pos) < 0) { // if not found in deleted
                    long ptr = Buffer.writeObject(file, car, wasZipped[0]);
                    idx.put(car, ptr);
                }
            }
        }
    }
}
