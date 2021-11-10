import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.FileStorage;
import model.User;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;

public class LocalImplementation extends SpecificationClass implements SpecificationInterface {
    HashMap<String, Long> mapOfStorageSizes = new HashMap<>();
    HashMap<String, Integer> mapOfDirRestrictions = new HashMap<>();
    StringBuilder jsonString = new StringBuilder();
    File users;
    File config;
    String osSeparator = File.separator;
    User connectedUser;
    FileStorage storage=new FileStorage();
   // ObjectMapper objectMapper = new ObjectMapper();


    // Djokic: sort i ls treba da vracaju niz!!! config fajl!

    // Kada koristimo Petrov kod, USERS.JSON se pravi bez uglastih zagrada. Da li je to ispravno?
    // onda nam puca prilikom logovanja jer ne ucita pravilno users.json
    // njegovo sam ostavila zakomentarisano

    // treba ubaciti neki errorhandler u specifikaciju i dodati sve exceptione

    static {
        SpecificationManager.registerExporter(new LocalImplementation());
    }

    @Override
    public void createFile(String filename) throws IOException {
        if (connectedUser.getLevel() < 4) {
            File newFile = new File(getStorage().getCurrentPath() + osSeparator + filename);

            // Ovo radi samo za jedno skladiste jer ne izvlaci podatke iz config.json
            if (storage.getRestriction() != null) {
                if (filename.endsWith(storage.getRestriction())) {
                   // System.out.println("You cannot make file with " + storage.getRestriction() + " extension");
                    return;
                }
            }

            if (mapOfDirRestrictions.containsKey(getStorage().getCurrentPath()) == true) {
                Integer numberOfFilesLeft = mapOfDirRestrictions.get(getStorage().getCurrentPath());
                if (numberOfFilesLeft > 0) {
                    newFile.createNewFile();
                    numberOfFilesLeft--;
                    mapOfDirRestrictions.put(getStorage().getCurrentPath(), numberOfFilesLeft);
                }
                //else System.out.println("Folder is full!");
            } else {
                newFile.createNewFile();
            }
        } else unauthorizedAction();
    }

    @Override
    public void createDirectory(String directoryName, Integer... restriction) {
        if (connectedUser.getLevel() < 4) {
            if (restriction.length > 0) {
                mapOfDirRestrictions.put(getStorage().getCurrentPath() + osSeparator + directoryName, restriction[0]);
            }
            File newDir = new File(getStorage().getCurrentPath() + osSeparator + directoryName);

            if (mapOfDirRestrictions.containsKey(getStorage().getCurrentPath()) == true) {
                Integer numberOfFilesLeft = mapOfDirRestrictions.get(getStorage().getCurrentPath());
                if (numberOfFilesLeft > 0) {
                    newDir.mkdir();
                    mapOfDirRestrictions.put(getStorage().getCurrentPath(), --numberOfFilesLeft);
                }
                //else System.out.println("Folder is full!");
            } else {
                newDir.mkdir();
            }
        } else unauthorizedAction();
    }

    @Override
    public void createStorage(String name, String path, Long storageSize, String... restrictions) {
        if (restrictions.length > 0) storage.setRestriction(restrictions[0]);

        jsonString = new StringBuilder();
        mapOfStorageSizes.put(path, storageSize);
        getStorage().setStoragePath(path + osSeparator + name);

        File storageFile = new File(getStorage().getStoragePath());
        storageFile.mkdir();
        String rootDirPath = getStorage().getStoragePath() + osSeparator + "rootDirectory";
        File rootDirectory = new File(rootDirPath);
        rootDirectory.mkdir();
        users = new File(rootDirectory + osSeparator + "users.json");
        config = new File(rootDirectory + osSeparator + "config.json");
        try {
            users.createNewFile();
            config.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        getStorage().setCurrentPath(getStorage().getStoragePath());
    }

    @Override
    public void createListOfDirectories(String dirName, Integer numberOfDirectories) {
        if (connectedUser.getLevel() < 4) {
            for (int i = 0; i < numberOfDirectories; i++) {
                createDirectory(dirName + i);
            }
            if (numberOfDirectories == 0) createDirectory(dirName + '0');

        }else unauthorizedAction();
    }
    @Override
    public void createListOfDirRestriction(String dirName, Integer restriction, Integer numberOfDirectories) {
        if (connectedUser.getLevel()<4) {
        for (int i = 0; i < numberOfDirectories; i++) {
            createDirectory(dirName + i, restriction);
        }
        }else unauthorizedAction();
    }

    @Override
    public void createListOfFiles(String filename, Integer numberOfFiles) {
        if (connectedUser.getLevel()<4) {
            for (int i = 0; i < numberOfFiles; i++) {
                try {
                    createFile(filename + i);
                } catch (IOException e) {
                    //System.out.println("Error: File not created");
                }
            }
        } else unauthorizedAction();
    }

    @Override
    public void createUser(String username, String password, Integer level) {
        if ((connectedUser == null) || connectedUser.getLevel() == 1) {
            try {

//                File file=new File(getStorage().getStoragePath() + osSeparator + "rootDirectory" + osSeparator + "users.json");
//                    String json= objectMapper.writeValueAsString(new User(username, password, level));
//                if (!file.exists()){
//                    Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.CREATE);
//                }else {
//                    Files.write(file.toPath(), Arrays.asList(json), StandardOpenOption.APPEND);
//                }

                Gson gson = new Gson();
                User user = new User(username, password, level);
                if (new File(getStorage().getStoragePath() + osSeparator + "rootDirectory" + osSeparator + "users.json").length() == 0) {
                    FileWriter file = new FileWriter(getStorage().getStoragePath() + osSeparator + "rootDirectory" + osSeparator + "users.json");
                    jsonString.append("[");
                    jsonString.append(gson.toJson(user));
                    jsonString.append("]");
                    file.write(String.valueOf(jsonString));
                    file.close();
                }
                else {
                    BufferedReader reader = new BufferedReader(new FileReader(getStorage().getStoragePath() + osSeparator + "rootDirectory" + osSeparator + "users.json"));
                    jsonString = new StringBuilder(reader.readLine());
                    jsonString.deleteCharAt(jsonString.length() - 1);
                    jsonString.append(",");
                    jsonString.append(gson.toJson(user));
                    jsonString.append("]");
                    //System.out.println(jsonString);
                    FileWriter file = new FileWriter(getStorage().getStoragePath() + osSeparator + "rootDirectory" + osSeparator + "users.json");
                    file.write(String.valueOf(jsonString));
                    file.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }  else unauthorizedAction();
    }
    @Override
    public boolean goForward(String filename){
        if (!isStorage(getStorage().getCurrentPath()+osSeparator+filename)) {
            getStorage().setCurrentPath(getStorage().getCurrentPath() + osSeparator + filename);
            return true;
        }else {
            getStorage().setStoragePath(getStorage().getCurrentPath()+osSeparator+filename);
            return false;
        }
    }
    @Override
    public void goBackwards(){
        String currentPath= getStorage().getCurrentPath();

        String separator[] = currentPath.split(Pattern.quote(osSeparator));
                    currentPath = "";
                    for (int i = 0 ; i < separator.length-1 ; i++)
                    {
                        currentPath += separator[i];
                        if (i != separator.length - 2) currentPath += osSeparator;
                    }


        getStorage().setCurrentPath(currentPath);
    }

    @Override
    public void moveFile(String filename, String newPath) {
        if (connectedUser.getLevel()<3) {
                    if (newPath.contains(getStorage().getStoragePath()) &&
                        !newPath.contains("rootDirectory")) {
                        try {
                            Files.move(Paths.get(getStorage().getCurrentPath() + osSeparator+filename),
                                    Paths.get(newPath + osSeparator+filename), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {

                        }

                    } else if (newPath.contains("rootDirectory")){
                        //System.out.println("You cannot move files to rootDirectory");
                        //throw exception
                    }

        }else unauthorizedAction();
    }

    @Override
    public boolean copyFile(String filename, String newPath){
        if (connectedUser.getLevel()<3) {
            Path newDir=Paths.get(newPath);
            if (newPath.contains(getStorage().getStoragePath()) && !newPath.contains("rootDirectory")){
                try {
                    Files.copy(Paths.get(getStorage().getCurrentPath() + osSeparator + filename), newDir.resolve(filename),
                            StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //throwException da ne moze da menja van skladista
            //throw exception ne moze rootDirectory
           // System.out.println("nema copy van skladista ne ne ne");
        } else unauthorizedAction();
        return false;
    }

    @Override
    public boolean uploadFile(String filename){

        if (copyFile(filename,getStorage().getStoragePath())) {
            // getStorage().setCurrentPath(getStorage().getStoragePath());
            return true;
        }
        return false;
    }

    @Override
    public boolean downloadFile(String filename) {
        if (connectedUser.getLevel()<3) {
            Path downloadDir=Paths.get(System.getProperty("user.home")+osSeparator+"StorageDownloads");
            if (!Files.exists(downloadDir)){
                File newDir = new File(System.getProperty("user.home") + osSeparator + "StorageDownloads");
                newDir.mkdir();
            }
            try {
                Files.copy(Paths.get(getStorage().getCurrentPath() + osSeparator + filename), downloadDir.resolve(filename),
                        StandardCopyOption.REPLACE_EXISTING);
                return true;

            } catch (IOException e) {
                    e.printStackTrace();
            }
        } else unauthorizedAction();
        return false;
    }

    @Override
    public void deleteFile(String filename) {
        if (connectedUser.getLevel()<4) {
            File file = new File(getStorage().getCurrentPath() + osSeparator + filename);
            if (file.isDirectory()) {
                deleteDirectory(file);
            }
            boolean deleted = file.delete();
          //  if (deleted == false) System.out.println("File is not in this folder.");

            if (mapOfDirRestrictions.containsKey(getStorage().getCurrentPath()) == true) {
                Integer numberOfFilesLeft = mapOfDirRestrictions.get(getStorage().getCurrentPath());
                mapOfDirRestrictions.put(getStorage().getCurrentPath(), ++numberOfFilesLeft);
            }
        } else unauthorizedAction();

    }


    public void deleteDirectory(File file) {
        if (connectedUser.getLevel() < 4) {
            for (File subfile : file.listFiles()) {
                if (subfile.isDirectory()) {
                    deleteDirectory(subfile);
                }
                subfile.delete();
            }
        } else unauthorizedAction();
    }

    @Override
    public void listFilesFromDirectory(String... extension) {
        File file = new File(getStorage().getCurrentPath());
        File[] list = file.listFiles();
        if (list != null) {
            for (File f : list) {
                if (extension.length == 0) System.out.println(f.getName());
                else {
                    for (String extensionName : extension) {
                        if (f.getName().endsWith(extensionName))
                            System.out.println(f.getName());

                        //ovde napraviti da vraca listu
                    }
                }
            }
        }
    }

    //sort treba da mi vraca niz kroz koji cu ja proci u komandnoj liniji i koji cu ispisati
    @Override
    public void sort(String order, String ... option) {
        File file = new File(getStorage().getCurrentPath());
        File[] list = file.listFiles();
        if (option.length == 0) {
            if (order.equals("asc")) {
                Arrays.sort(list);
            }
            else if (order.equals("desc")) {
                Arrays.sort(list, Collections.reverseOrder());
            }
        }
        else {
            if (option[0].equals("date")) {
                if (order.equals("asc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::lastModified));
                }
                else if (order.equals("desc")) {
                    Arrays.sort(list, Comparator.comparingLong(File::lastModified).reversed());
                }
            }
            else if (option[0].equals("size")) {
                HashMap<String, Long> mapOfFiles = new HashMap<>();
                for (File f : list) {
                    if (f.isDirectory()) mapOfFiles.put(f.getName(), FileUtils.sizeOfDirectory(f));
                    else mapOfFiles.put(f.getName(), f.length());
                }
                if (order.equals("asc")) {
                    mapOfFiles.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue())
                            .forEach(System.out::println);
                }
                else if (order.equals("desc")) {
                    mapOfFiles.entrySet()
                            .stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .forEach(System.out::println);
                }
            }
        }
    }

    @Override
    public void editFile(String filename) {
        File file = new File(getStorage().getCurrentPath()+osSeparator+filename);
        if (connectedUser.getLevel() < 4) file.setWritable(true);
        else file.setWritable(false);

        try {
            Desktop.getDesktop().open(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean logIn(String username, String password) {
        String path=getStorage().getStoragePath()+osSeparator+ "RootDirectory";

        if (new File(path + osSeparator + "users.json").length() == 0) {
            createUser(username, password, 1);
            connectedUser = new User(username,password,1);
            getStorage().setCurrentPath(getStorage().getStoragePath());
            return true;

        }
        else {
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                BufferedReader reader = new BufferedReader(new FileReader(
                        path + osSeparator + "users.json"));
                Type userListType = new TypeToken<ArrayList<User>>() {}.getType();
                ArrayList<User> userArray = gson.fromJson(reader,userListType);
                for (User user: userArray) {
                    if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                        connectedUser = new User(username, password, user.getLevel());
                        getStorage().setCurrentPath(getStorage().getStoragePath());
                        return true;

                    }
                }
                reader.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void logOut() {
        connectedUser = null;
    }



    @Override
    public boolean isStorage(String currentPath) {
        String users = currentPath + osSeparator + "rootDirectory" + osSeparator + "users.json";
        String config = currentPath + osSeparator + "rootDirectory" + osSeparator + "config.json";
        Path pathToUsers = Paths.get(users);
        Path pathToConfig = Paths.get(config);
        if (Files.exists(pathToUsers) && Files.exists(pathToConfig)) return true;
        else return false;
    }

    @Override
    public User getConnectedUser() {
        return connectedUser;
    }

    @Override
    public FileStorage getStorage() {
        return storage;
    }

    public void unauthorizedAction(){
        System.out.println("Unauthorized action. Level: " + connectedUser.getLevel());
    }
}
