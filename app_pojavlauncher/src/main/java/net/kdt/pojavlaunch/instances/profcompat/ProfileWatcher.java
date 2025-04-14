package net.kdt.pojavlaunch.instances.profcompat;

import net.kdt.pojavlaunch.Tools;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

public class ProfileWatcher {
    private static final File sLauncherProfiles = new File(Tools.DIR_GAME_NEW, "launcher_profiles.json");
    public static String consumePendingVersion() throws IOException {
        Profiles store;
        try(FileReader fileReader = new FileReader(sLauncherProfiles)) {
            store = Tools.GLOBAL_GSON.fromJson(fileReader, Profiles.class);
        }
        Map<String, ProfileBody> profiles = store.profiles;
        String versionId = null;
        Iterator<Map.Entry<String, ProfileBody>> iter = profiles.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<String, ProfileBody> entry = iter.next();
            if("(Default)".equals(entry.getKey())) continue;
            if(versionId == null) versionId = entry.getValue().lastVersionId;
            iter.remove();
        }
        try (FileWriter fileWriter = new FileWriter(sLauncherProfiles)) {
            Tools.GLOBAL_GSON.toJson(store, fileWriter);
        }
        return versionId;
    }
}
