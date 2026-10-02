import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;

/** Test-only observer. Not included in any published mod jar. */
public final class ClientBootAgent {
    public static void premain(String ignored, Instrumentation instrumentation) {
        Thread observer = new Thread(() -> observe(instrumentation), "smartvillagers-client-boot-test");
        observer.setDaemon(true);
        observer.start();
    }

    private static void observe(Instrumentation instrumentation) {
        int stable = 0;
        String lastScreen = "";
        String lastReadyState = "";
        try {
            while (true) {
                Thread.sleep(1000);
                Object client = null;
                Class<?> minecraft = null;
                Class<?> mod = null;
                for (Class<?> type : instrumentation.getAllLoadedClasses()) {
                    if (type.getName().equals("net.minecraft.client.Minecraft")
                            || type.getName().equals("net.minecraft.class_310")) {
                        minecraft = type;
                    }
                    if (type.getName().equals("tn.naizo.smartvillagers.SmartVillagers")) {
                        mod = type;
                    }
                }
                if (minecraft == null || mod == null) continue;
                // Forge's event transformer uses the current thread's context loader.
                Thread.currentThread().setContextClassLoader(minecraft.getClassLoader());
                for (Method method : minecraft.getDeclaredMethods()) {
                    if (Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0
                            && method.getReturnType() == minecraft) {
                        method.setAccessible(true);
                        client = method.invoke(null);
                        break;
                    }
                }
                if (client == null || mod == null) continue;
                Object gui = client;
                boolean screenOnClient = false;
                for (Field field : client.getClass().getDeclaredFields()) {
                    if (field.getType().getName().equals("net.minecraft.client.gui.screens.Screen")
                            || field.getType().getName().equals("net.minecraft.class_437")) screenOnClient = true;
                }
                if (!screenOnClient) {
                    for (Field field : client.getClass().getDeclaredFields()) {
                        if (field.getType().getName().equals("net.minecraft.client.gui.Gui")) {
                            field.setAccessible(true);
                            gui = field.get(client);
                        }
                    }
                }
                if (gui == null) continue;
                Object screen = currentField(gui, "net.minecraft.client.gui.screens.Screen", "net.minecraft.class_437");
                Object overlay = currentField(gui, "net.minecraft.client.gui.screens.Overlay", "net.minecraft.class_4071");
                String screenState = (screen == null ? "null" : screen.getClass().getName()) + "/"
                        + (overlay == null ? "null" : overlay.getClass().getName());
                if (!screenState.equals(lastScreen)) {
                    System.out.println("[SV BOOT TEST] current screen/overlay: " + screenState);
                    lastScreen = screenState;
                }
                boolean title = screen != null && (screen.getClass().getName().equals("net.minecraft.client.gui.screens.TitleScreen")
                        || screen.getClass().getName().equals("net.minecraft.class_442"));
                Object level = currentField(client, "net.minecraft.client.multiplayer.ClientLevel", "net.minecraft.class_638");
                Object player = currentField(client, "net.minecraft.client.player.LocalPlayer", "net.minecraft.class_746");
                boolean world = level != null && player != null;
                String readyState = overlay != null ? "WAIT" : title ? "TITLE" : world ? "WORLD" : "WAIT";
                stable = readyState.equals("WAIT") ? 0 : readyState.equals(lastReadyState) ? stable + 1 : 1;
                lastReadyState = readyState;
                if (stable < 10) continue;
                Field initialized = mod.getDeclaredField("initialized");
                initialized.setAccessible(true);
                if (!initialized.getBoolean(null)) continue;
                ClassLoader loader = mod.getClassLoader();
                Class<?> config = loader.loadClass("tn.naizo.smartvillagers.config.SmartVillagersConfig");
                Object snapshot = config.getMethod("get").invoke(null);
                Class<?> snapshotType = snapshot.getClass();
                if (!snapshotType.getMethod("provider").invoke(snapshot).toString().equals("CUSTOM"))
                    throw new IllegalStateException("Provider config was not loaded");
                Class<?> provider = loader.loadClass("tn.naizo.smartvillagers.ai.OpenAiCompatibleProvider");
                Method send = provider.getDeclaredMethod("send", snapshotType, String.class, String.class, String.class);
                send.setAccessible(true);
                Object response = ((java.util.concurrent.CompletableFuture<?>) send.invoke(null, snapshot, "probe-key", "system", "hello"))
                        .get(15, java.util.concurrent.TimeUnit.SECONDS);
                if (!Boolean.TRUE.equals(response.getClass().getMethod("ok").invoke(response))
                        || !"Hello traveler!".equals(response.getClass().getMethod("text").invoke(response)))
                    throw new IllegalStateException("Release jar HTTP contract failed");
                Path result = Path.of(System.getProperty("smartvillagers.smoke.result"));
                Files.writeString(result, "PASS state=" + readyState + " stableSeconds=10 loadingOverlay=false modInitialized=true httpContract=true\n");
                System.out.println("[SV BOOT TEST] PASS stable client, initialized mod, release jar HTTP request/response verified");
                // Let the render thread close its own window and resources.
                Object finishedClient = client;
                Method stop = null;
                for (String name : new String[]{"stop", "method_1592", "m_91395_"}) {
                    try {
                        stop = client.getClass().getDeclaredMethod(name);
                        break;
                    } catch (NoSuchMethodException ignored) { }
                }
                if (stop == null) throw new IllegalStateException("Client stop method missing");
                Method finishedStop = stop;
                ((java.util.concurrent.Executor) client).execute(() -> {
                    try {
                        finishedStop.invoke(finishedClient);
                    } catch (ReflectiveOperationException error) {
                        error.printStackTrace();
                        Runtime.getRuntime().halt(2);
                    }
                });
                return;
            }
        } catch (Throwable error) {
            error.printStackTrace();
            Runtime.getRuntime().halt(2);
        }
    }

    private static Object currentField(Object value, String namedType, String fabricType) throws ReflectiveOperationException {
        for (Class<?> type = value.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                String fieldType = field.getType().getName();
                if (!Modifier.isStatic(field.getModifiers()) && (fieldType.equals(namedType) || fieldType.equals(fabricType))) {
                    field.setAccessible(true);
                    return field.get(value);
                }
            }
        }
        throw new IllegalStateException("Current screen/overlay field not found: " + namedType);
    }

}
