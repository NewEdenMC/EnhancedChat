package co.neweden.enhancedchat.playerdata;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class Group implements Comparable<Group> {

    private String machineName;
    private String name;
    private int rank;
    Collection<ProxiedPlayer> members = new HashSet<>();

    Group(String machineName, String name, int rank) {
        this.machineName = machineName;
        this.name = name;
        this.rank = rank;
    }

    public String getMachineName() { return machineName; }

    public String getName() { return name; }

    public int getRank() { return rank; }

    public Collection<ProxiedPlayer> getMembers() {
        return Collections.unmodifiableCollection(members);
    }

    @Override
    public int compareTo(Group another) {
        // First compare the rank to see if they are different
        int rank = Integer.compare(this.getRank(), another.getRank());
        if (rank != 0) return rank;
        // Then compare the name
        return getMachineName().compareTo(another.getMachineName());
    }

}
