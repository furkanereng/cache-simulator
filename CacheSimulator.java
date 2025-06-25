/* CSE2138 - Project #3 - Spring 2025
 * Yasin Emre Çetin
 * Eren Emre Aycibin
 * Furkan Eren Gülçay
 * 
 * This program simulates the caches. 
 * Prints where the data placed to or stored in, in the 'output.txt' file.
 * Prints the total number of hits, misses and evictions to the console.
 * Prints the final contents of each caches to separate text files.
 */

import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.io.File;

public class CacheSimulator {
    static FileWriter writer;
    static String traceFileName;
    static Cache L1I, L1D, L2;
    static RAM RAM;

    public static void main(String[] args) {
        try {
            initialize(args);
            simulate();
            cacheToFile(L1D, "L1D");
            cacheToFile(L1I, "L1I");
            cacheToFile(L2, "L2");
            System.out.println("L1I-hits:" + L1I.hits + " L1I-misses:" + L1I.misses + " L1I-evictions:" + L1I.evictions);
            System.out.println("L1D-hits:" + L1D.hits + " L1D-misses:" + L1D.misses + " L1D-evictions:" + L1D.evictions);
            System.out.println("L2-hits:" + L2.hits + " L2-misses:" + L2.misses + " L2-evictions:" + L2.evictions);
        } catch (Exception e) {
            System.out.println("An error about the trace file, 'RAM.dat' file or output files is detected!");
            System.exit(5);
        }
    }

    /* Reads the command-line arguments. Creates the caches and RAM. Sets the trace file. */
    public static void initialize(String args[]) throws Exception {
        int L1s = -1, L1E = -1, L1b = -1, L2s = -1, L2E = -1, L2b = -1;
        traceFileName = null;
        if (args.length != 14) {
            System.out.println("Usage: java CacheSimulator -L1s <L1s> -L1E <L1E> -L1b <L1b> -L2s <L2s> -L2E <L2E> -L2b <L2b> -t <tracefile>");
            System.exit(1);
        }

        try {
            for (int i = 0; i < args.length; i++) {
                switch(args[i++]) {
                    case "-L1s": L1s = Integer.parseInt(args[i]); break;
                    case "-L1E": L1E = Integer.parseInt(args[i]); break;
                    case "-L1b": L1b = Integer.parseInt(args[i]); break;
                    case "-L2s": L2s = Integer.parseInt(args[i]); break;
                    case "-L2E": L2E = Integer.parseInt(args[i]); break;
                    case "-L2b": L2b = Integer.parseInt(args[i]); break;
                    case "-t": traceFileName = args[i]; break;
                    default:
                        System.out.println("Invalid argument is detected!");
                        System.exit(2);
                }
            }
        } catch (Exception e) {
            System.out.println("Invalid input is detected!");
            System.exit(3);
        }

        if (L1s < 0 || L1E < 0 || L1b < 0 || L2s < 0 || L2E < 0 || L2b < 0 || !traceFileName.contains(".trace")) {
            System.out.println("Invalid input is detected!");
            System.exit(4);
        }

        L1I = new Cache(L1s, L1E, L1b);
        L1D = new Cache(L1s, L1E, L1b);
        L2  = new Cache(L2s, L2E, L2b);
        RAM = new RAM();
    }

    /* Reads the trace file and simulates the trace. */
    public static void simulate() throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(traceFileName));
        writer = new FileWriter("output.txt");
        String line = null;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty())
                continue;
            
            String lineContent[] = line.split("[ ,]+"); /* This regular expression splits the line content according to one or more " " and "," characters. */
            char op = lineContent[0].charAt(0);
            int addr = Integer.parseInt(lineContent[1], 16);
            int size = Integer.parseInt(lineContent[2]);
            String data;

            writer.write(line + "\n");
            switch(op) {
                /* Instruction Load */
                case 'I': 
                    load(L1I, addr, size);
                    break;

                /* Data Load */
                case 'L': 
                    load(L1D, addr, size);
                    break;

                /* Data Store */
                case 'S':
                    data = lineContent[3];
                    store(addr, size, data);
                    break;

                /* Data Modify */
                case 'M': 
                    data = lineContent[3];
                    load(L1D, addr, size);
                    writer.write("\n");
                    store(addr, size, data);
                    break;

                /* Invalid trace file format */
                default:
                    System.out.println("Error about the trace file is detected!");
                    System.exit(7);
            }
            writer.write("\n");
        }
        reader.close();
        writer.close();
    }

    /* Loads data to the caches. Prints the place ins to the 'output.txt'. */
    public static void load(Cache L1, int addr, int size) throws Exception {
        Status L1Status = L1.getStatus(addr, false);
        Status L2Status = L2.getStatus(addr, false);

        String cacheName = (L1 == L1D) ? "L1D" : "L1I";
        String stat = (L1Status == Status.HIT) ? "hit" : "miss";
        writer.write("\t" + cacheName + " " + stat + ", L2 ");
        stat = (L2Status == Status.HIT) ? "hit" : "miss";
        writer.write(stat);

        Set setL1 = L1.getSet(addr), setL2 = L2.getSet(addr);
        int setL1index = L1.getSetIndex(addr), setL2index = L2.getSetIndex(addr);
        int tagL1 = L1.getTag(addr), tagL2 = L2.getTag(addr);
        int offsetL1 = L1.getOffset(addr), offsetL2 = L2.getOffset(addr);

        /* If a HIT is detected in L1, load the data from L1. */
        if (L1Status == Status.HIT) {
            setL1.load(size, tagL1, offsetL1);
            L1.updateHits();
            return;

        /* If a MISS or EVIDENCE detected in L1... */
        } else {
            L1.updateMisses();
            if (L1Status == Status.EVIDENCE) 
                L1.updateEvictions();
            writer.write("\n\tPlace in ");
            
            Line lineL1 = setL1.getLine();
            int startAddrL1 = addr - offsetL1;
            int startAddrL2 = addr - offsetL2;
            int offset = startAddrL2 - startAddrL1;

            lineL1.tag = tagL1;
            lineL1.valid = true;

            /* ... and if a HIT is detected in L2, load the data from L2 to L1. */
            if (L2Status == Status.HIT) {
                Line lineL2 = setL2.getLine(tagL2);
                setL2.load(size, tagL2, offsetL2);
                lineL1.updateBlock(lineL2.block, offset);
                lineL1.updateTime();
                L2.updateHits();
                writer.write(cacheName + " set " + setL1index);
                return;

            /* ... and if a MISS or EVIDENCE detected in L2; first load the data from RAM to L2, then L2 to L1. */
            } else {
                L2.updateMisses();
                if (L2Status == Status.EVIDENCE)
                    L2.updateEvictions();

                Line lineL2 = setL2.getLine();
                lineL2.tag = tagL2;
                lineL2.valid = true;

                lineL2.block = RAM.load(addr, lineL2);
                setL2.load(size, tagL2, offsetL2);
                lineL1.updateBlock(lineL2.block, offset);

                lineL2.updateTime();
                lineL1.updateTime(lineL2.time);
                writer.write("L2 set " + setL2index + ", " + cacheName + " set " + setL1index);
                return;
            }
        }
    }

    /* Stores the input data in the caches and RAM, according to no-write allocate and write-through policies. */
    public static void store(int addr, int size, String data) throws Exception {
        Status L1Status = L1D.getStatus(addr, true);
        Status L2Status = L2.getStatus(addr, true);

        String stat = (L1Status == Status.HIT) ? "hit" : "miss";
        writer.write("\tL1D " + stat + ", L2 ");
        stat = (L2Status == Status.HIT) ? "hit" : "miss";
        writer.write(stat + "\n\tStore in");

        Set setL1 = L1D.getSet(addr), setL2 = L2.getSet(addr);
        int tagL1 = L1D.getTag(addr), tagL2 = L2.getTag(addr);
        int offsetL1 = L1D.getOffset(addr), offsetL2 = L2.getOffset(addr);

        String temp[] = new String[data.length() / 2];
        for (int i = 0; i < temp.length; i++)
            temp[i] = data.substring(2 * i, 2 * i + 2);

        /* If a HIT is detected in L1, update L1D. Otherwise, just update its misses. */
        if (L1Status == Status.HIT) {
            writer.write(" L1D,");
            L1D.updateHits();
            Line lineL1 = setL1.getLine(tagL1);
            lineL1.store(temp, offsetL1);
        } else {
            L1D.updateMisses();
        }

        /* If a HIT is detected in L2, update L2. Otherwise, just update its misses. */
        if (L2Status == Status.HIT) {
            writer.write(" L2,");
            L2.updateHits();
            Line lineL2 = setL2.getLine(tagL2);
            lineL2.store(temp, offsetL2);
        } else {
            L2.updateMisses();
        }

        /* Finally, update the RAM. */
        writer.write(" RAM");
        RAM.store(addr, data);
    } 

    /* Prints the content of the input cache to the corresponding file. */
    public static void cacheToFile(Cache cache, String cacheName) throws Exception {
        writer = new FileWriter(cacheName + ".txt");
        for (int i = 0; i < cache.sets.length; i++) {
            writer.write("SET " + i + ":\n");
            for (int j = 0; j < cache.sets[i].lines.length; j++) {
                Line line = cache.sets[i].lines[j];
                int valid = (line.valid) ? 1 : 0;
                String data = "";
                for (int k = 0; k < line.block.length; k++)
                    data += line.block[k];
                data = data.replaceAll("null", "XX"); /* For invalid lines, prints XX...XX to the data. */
                writer.write("Line " + j + " | time: " + line.time + ", valid: " + valid);
                writer.write(", tag: " + line.tag + ", data: " + data + "\n");
            }
            writer.write("\n");
        }
        writer.close();
    }

    static class Cache {
        static int time = 0; /* The current time for all of the caches. */
        int hits, misses, evictions; 
        Set sets[]; 
        int s, b; 

        /* Initializes the cache and its sets. */
        public Cache(int s, int E, int b) {
            this.s = s;
            this.b = b;
            this.sets = new Set[1 << s];
            for (int i = 0; i < sets.length; i++)
                sets[i] = new Set(E, b);
        }

        /* Returns HIT, MISS or EVIDENCE to determine the status of the cache for the instruction. */
        public Status getStatus(int addr, boolean isStore) {
            Set set = getSet(addr);
            int tag = getTag(addr);

            /* Checks that is it a hit or not. */
            for (Line line : set.lines) 
                if (line.valid && line.tag == tag)
                    return Status.HIT;
        
            /* If it is not a hit and it is store operation, it is definitely a miss. */
            if (isStore)
                return Status.MISS;

            /* If it is not a hit and it is load operation, checks that is there an empty line. */
            for (Line line : set.lines)
                if (!line.valid)
                    return Status.MISS;

            /* If there is also no empty line, it will be an evidence. */
            return Status.EVIDENCE;
        }

        /* Returns the corresponding set for the input address. */
        public Set getSet(int addr) {
            return sets[getSetIndex(addr)];
        }

        /* Returns the index of the corresponding set for the input address. */
        public int getSetIndex(int addr) {
            return (addr >> b) & ((1 << s) - 1);
        }

        /* Returns the tag for the input address. */
        public int getTag(int addr) {
            return addr >> (s + b);
        }

        /* Returns the offset to determine where the address is located. */
        public int getOffset(int addr) {
            return addr & ((1 << b) - 1);
        }

        public void updateHits() {
            hits += 1;
        }

        public void updateMisses() {
            misses += 1;
        }

        public void updateEvictions() {
            evictions += 1;
        }
    }

    static class Set {
        Line lines[];

        /* Initializes the lines of the set. */
        public Set(int E, int b) {
            this.lines = new Line[E];
            for (int i = 0; i < lines.length; i++)
                lines[i] = new Line(b);
        }

        /* Loads the data from the corresponding line of the cache. */
        public String[] load(int size, int tag, int offset) {
            String data[] = new String[size];
            for (Line line : lines)
                if (line.valid && line.tag == tag)
                    data = Arrays.copyOfRange(line.block, offset, offset + size);
            return data;
        }

        /* Returns the corresponding line for the input tag. */
        public Line getLine(int tag) {
            for (Line line : lines)
                if (line.valid && line.tag == tag)
                    return line;
            return null;
        }

        /* Returns the first empty line or the line that will be updated in the case of evidence occured. */
        public Line getLine() {
            for (Line line : lines)
                if (!line.valid)
                    return line;

            Line firstLine = lines[0];
            for (Line line : lines)
                if (line.time < firstLine.time)
                    firstLine = line;
            return firstLine;
        }
    }

    static class Line {
        boolean valid;  /* Indicates that is the data inside the line valid or not. */
        int time, tag;  /* The tag value of the line and the time for the line. */
        String block[]; /* Stores the data byte-by-byte, as a string. */

        /* Allocates the memory space for the data of the line. */
        public Line(int b) {
            this.block = new String[1 << b];
        }

        /* Updates the whole block from a bigger/equal sized data. */
        public void updateBlock(String[] data, int offset) {
            for (int i = 0; i < block.length; i++)
                block[i] = data[offset + i];
        }

        /* Stores the data in its correct place in the block. */
        public void store(String[] data, int offset) {
            for (int i = 0; i < data.length; i++)
                block[offset + i] = data[i];
        }

        public int getBlockSize() {
            return block.length;
        }

        public void updateTime() {
            this.time = ++Cache.time;
        }

        public void updateTime(int time) {
            this.time = time;
        }
    }

    static class RAM {
        String content[];

        /* Initializes the RAM content from 'RAM.dat'. */
        public RAM() throws Exception {
            File file = new File("RAM.dat");
            byte[] rawBytes = new byte[(int)file.length()];

            try (FileInputStream fis = new FileInputStream(file)) {
                fis.read(rawBytes);
            }

            String[] hexStrings = new String[rawBytes.length];
            for (int i = 0; i < rawBytes.length; i++) {
                hexStrings[i] = String.format("%02X", rawBytes[i] & 0xFF);
            }
            this.content = hexStrings;
        }

        /* Gets the aligned data from the RAM. */
        public String[] load(int addr, Line line) {
            int blockSize = line.getBlockSize();
            int alignedAddr = addr - (addr % blockSize);
            String data[] = new String[blockSize];
            for (int i = 0; i < blockSize; i++)
                data[i] = content[alignedAddr + i];
            return data; 
        }

        /* Stores the data in the RAM. */
        public void store(int addr, String data[]) {
            for (int i = 0; i < data.length; i++)
                content[addr + i] = data[i]; 
        }

        /* Stores the data in the RAM. (Just as the previous one) */
        public void store(int addr, String data) {
            for (int i = 0; i < data.length() / 2; i++)
                content[addr + i] = data.substring(2 * i, 2 * i + 2);
        }
    }

    /* Used for readability of the code. */
    static enum Status {
        HIT, MISS, EVIDENCE
    }
}
