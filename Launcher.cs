using System;
using System.IO;
using System.Diagnostics;
using System.Reflection;
using System.IO.Compression;
using System.Security.Cryptography;

public class Launcher {
    [STAThread]
    public static void Main(string[] args) {
        try {
            Assembly assembly = Assembly.GetExecutingAssembly();
            string resourceName = null;
            foreach (string name in assembly.GetManifestResourceNames()) {
                if (name.EndsWith(".zip", StringComparison.OrdinalIgnoreCase)) {
                    resourceName = name;
                    break;
                }
            }
            
            if (resourceName == null) {
                System.Windows.Forms.MessageBox.Show("Embedded resource not found.", "Error", System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
                return;
            }
            
            string hashStr = "";
            using (Stream s = assembly.GetManifestResourceStream(resourceName)) {
                using (SHA256 sha = SHA256.Create()) {
                    byte[] hash = sha.ComputeHash(s);
                    hashStr = BitConverter.ToString(hash).Replace("-", "").ToLower();
                }
            }
            
            string appDirName = "zDwnld_" + hashStr.Substring(0, 12);
            string localAppData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
            string parentDir = Path.Combine(localAppData, "zDwnld_portable");
            string targetDir = Path.Combine(parentDir, appDirName);
            string exePath = Path.Combine(targetDir, "zDwnld.exe");
            
            if (!Directory.Exists(targetDir) || !File.Exists(exePath)) {
                if (Directory.Exists(parentDir)) {
                    try {
                        foreach (string oldDir in Directory.GetDirectories(parentDir, "zDwnld_*")) {
                            Directory.Delete(oldDir, true);
                        }
                    } catch {}
                }
                Directory.CreateDirectory(targetDir);
                
                using (Stream s = assembly.GetManifestResourceStream(resourceName))
                using (ZipArchive archive = new ZipArchive(s)) {
                    foreach (ZipArchiveEntry entry in archive.Entries) {
                        string destPath = Path.GetFullPath(Path.Combine(targetDir, entry.FullName));
                        if (!destPath.StartsWith(targetDir, StringComparison.OrdinalIgnoreCase)) {
                            continue;
                        }
                        
                        if (string.IsNullOrEmpty(entry.Name)) {
                            Directory.CreateDirectory(destPath);
                        } else {
                            Directory.CreateDirectory(Path.GetDirectoryName(destPath));
                            entry.ExtractToFile(destPath, true);
                        }
                    }
                }
            }
            
            ProcessStartInfo startInfo = new ProcessStartInfo();
            startInfo.FileName = exePath;
            if (args != null && args.Length > 0) {
                string[] escapedArgs = new string[args.Length];
                for (int i = 0; i < args.Length; i++) {
                    escapedArgs[i] = "\"" + args[i].Replace("\"", "\\\"") + "\"";
                }
                startInfo.Arguments = string.Join(" ", escapedArgs);
            }
            startInfo.WorkingDirectory = targetDir;
            Process.Start(startInfo);
        } catch (Exception ex) {
            System.Windows.Forms.MessageBox.Show("Failed to launch application:\n" + ex.ToString(), "Error", System.Windows.Forms.MessageBoxButtons.OK, System.Windows.Forms.MessageBoxIcon.Error);
        }
    }
}
