# AI Assistant Plugin for IntelliJ IDEA

> Easy to use tool to comment and correct code based on a free local AI

By hosting locally an AI, we can have useful functionalities without worrying about subscription based APIs.

---

## Features

### 💬 Code Commenting
Select code to send to our local AI. It will suggest comments inserted on top of each line to accurately explain how it all works and why. If you don't want the comments to be placed between your lines of code, it also offers the option of generating a one paragraph comment explaining the whole process, which will be placed on top of the code selected.

### ✅ Convention Checker
Select code to send to our local AI. It will check each line to make sure all variables, classes, interfaces, packages, etc; conform to Java conventions. It will also reformat the code so no line exceeds the specified character amount. Once it has processed it, it will show the suggested code, so that you can choose to apply the changes.

### ⚙️ Configurable Prompts and Settings
All prompts used in the plugin are configurable, in case you want to try doing your own prompt engineering to tailor results to your taste. You can also change the maximum characters per line for the convention checker. Remember to add `%s` where you want the code to be inserted in the prompt for it to work. There is also a button in settings to reset everything back to the default state.

### 🔧 Tweakable Limits
Since it is all based on your own device, the AI's capacity will be limited by your hardware. By default there are certain limitations you can change if your pc can handle them.

### ↩️ Undo Friendly
All actions can be undone with the usual `Ctrl+Z`.

### 🎨 UI Theme Friendly
Colors will adapt to the theme.

---

## Prerequisites & Setup

1. **Install Ollama** — Download from [ollama.com](https://ollama.com) and run `ollama pull llama3.2:latest` in your terminal.
2. **Build the Plugin** — Run `./gradlew clean buildPlugin` in the project root.
3. **Install in IntelliJ** — Go to `Settings → Plugins → Gear Icon → Install Plugin from Disk` and select the generated ZIP file.
4. **Configure** — Go to `Settings → Tools → Ollama AI` to verify the URL and model, then click Apply.
5. **Configure Ollama** so it doesn't start automatically; the plugin already offers to start it whenever you launch the IDE and shuts it down when you close it.

---

## How to Use

**Testing the connection**  
You can always check if the connection to the local AI has been successful with a quick test action. On the top bar, inside the tools menu you will find `Test Ollama Connection`. If everything is right, Ollama will answer with `"Hello"`.

**Settings**  
Inside `Settings` (`Ctrl+Alt+S`) → `Tools` → `Ollama AI`, you will find all configurable options. A brief setup guide appears by default (with a link to download Ollama); you can hide it with the checkbox.  
Here you can adjust: Ollama URL, model name, thinking-window text, prompts for inline/block comments, max line length, and selection character limit. Action shortcuts are also displayed and can be customized via IntelliJ's Keymap settings.  
There is also a link to the official Java conventions.

**Running a feature**  
To use any of the 2 features, you must first select the code you want to send to the AI. Then, either with the shortcut or in the right-click menu, select the feature you want.

---

### Code Commenting — `Ctrl+Alt+E`

After the AI has processed the code, you will see a window with the proposed version of the code. All your original code will be preserved, but you will see a `//` comment on top of every few lines, explaining the components. Since we are asking the AI to always explain, the less you select the less knowledge of coding it will assume, even explaining what `System.out.println` does if that is the only selected line.

After reviewing the text, you will see 3 buttons at the bottom of the window:

| Button | Action |
|---|---|
| **Apply Commented Version** | Replaces your original code in the file with the suggested code. |
| **Generate Summary Content** | Sends your original code to the AI again. After thinking, it will suggest a one paragraph `/* */` comment explaining the code. This is especially useful when you don't want comments cluttering your code. In this new window you will see the same buttons as before, except "Generate Summary Content". |
| **Dismiss** | Closes the window without applying changes, in case you change your mind. It is virtually the same as pressing the X button of the top right corner. |

> **Note:** This function does not support all convention approved comment styles.

---

### Convention Checker — `Ctrl+Alt+Shift+Z`

After the AI has processed the code, you will see a window with the proposed version of the code.

You will first see the original text, to make it easier to see what the AI changed or reformatted. It will also mark with `←CHANGED:` every line where a change has been made, so it is easier to identify what the AI changed.

In the window right below it you will see the suggested change. The AI will change all names to follow conventions, like camelCase. It will also jump fragments of code to the next line whenever a line exceeds the specified character amount (120 by default).

You will see 2 buttons, one to apply the changes and one to dismiss.

> **Note:** This function will NOT refactor your code; you will have to do that manually or by using IntelliJ's integrated methods. It is recommended to use this function only over declarations.

---

## How It Works

First, all settings are saved in a persistent state inside an XML, to keep the user's custom configuration. Before we start any action we create an object to "load" all the settings, making sure nothing has to be hard coded other than the defaults.

Since we can access Ollama through the console, we just have to establish a link to our local host in the correct port to send all prompts with an HTTP request.

The next difficulty is sending a prompt that includes text that can't be hardcoded. Instead of also using an XML, we establish a reference to replace with the code inside the prompt String. This way we can simply replace it.

After some prompt engineering, we end with a functionality that pretty reliably makes good comment suggestions, and reformats variables and classes to adhere to conventions.

---

## Technical Decisions

### General Implementation

**AI Connection**  
The plugin communicates with Ollama using `java.net.http.HttpClient`. POST requests are sent to `localhost:11434/api/generate` with a JSON payload containing the model identifier and prompt. The response is parsed to extract the `"response"` field, trimmed, and stripped of Markdown formatting before presentation.

**Persistent State**  
Configuration data is stored in `OllamaPluginSettings.xml` using IntelliJ's `PersistentStateComponent` interface. The `@State` annotation defines the storage file. On startup, `loadState()` maps saved XML values to the active instance. On shutdown, `getState()` returns the current instance for serialization. This ensures settings persist across IDE sessions without external configuration files.

**Threading and UI Responsiveness**  
Network operations execute on a background thread via `ApplicationManager.executeOnPooledThread()`. UI updates, dialog displays, and editor modifications are dispatched to the Event Dispatch Thread using `ApplicationManager.invokeLater()`. A modal `LoadingMessage` dialog appears during processing to indicate activity while keeping the main editor thread responsive.

---

### File Structure

```
src/
├── actions/
│   ├── commenting/
│   │   ├── CommentAction.java        # Entry point for the commenting feature
│   │   └── CommentResult.java        # Result dialog for commenting
│   └── convention/
│       ├── ConventionAction.java     # Entry point for the convention checker
│       └── ConventionResult.java     # Result dialog for convention checker
├── ai/
│   ├── OllamaClient.java             # HTTP communication with Ollama
│   └── AiResponseCleaner.java        # Response parsing and cleanup
├── settings/
│   ├── OllamaSettings.java           # Configuration persistence
│   └── OllamaConfigurable.java       # Settings UI panel
├── core/                             # Startup lifecycle components
└── ui/                               # Reusable UI components
```

Action entry points and result dialogs reside in dedicated action packages. HTTP communication and response parsing are centralized in `ai/`. Configuration persistence and UI are isolated in `settings/`. Startup lifecycle and reusable UI components are separated into `core/` and `ui/` respectively.

---

### UI

**Settings Window**  
The configuration panel is implemented as a `Configurable` using Swing components (`JTextField`, `JTextArea`, `JSpinner`) wrapped in IntelliJ JB variants for theme consistency. Layout is managed with `BoxLayout` and `GridBagLayout`. The `reset()` method populates fields from the active `OllamaSettings` instance. The `isModified()` method compares current field values against saved state. The `apply()` method validates numeric inputs and persists changes. A *Revert to Defaults* button instantiates a fresh `OllamaSettings` object to copy hardcoded default values back to the active state and UI.

**Toolbar Actions**  
Actions are registered in `plugin.xml` under `EditorPopupMenu` with default keyboard shortcuts. The `update()` method enables actions only when a valid `Project` and `Editor` are active, and the current file has a `.java` extension. Placement is controlled via `anchor` and `relative-to-action` attributes in the XML configuration.

---

### Action Logic

#### Explain Code — `CommentAction`

**Process:**
1. Validates selection size and file type.
2. Injects selected text into `explanationPrompt` at the `%s` placeholder.
3. Sends prompt to Ollama on a background thread.
4. Cleans response by removing whitespace and Markdown fences.
5. Displays a preview dialog with original and commented code.
6. Provides options to apply changes, generate a block summary, or dismiss.

**Challenges & Solutions:**

| Issue | Resolution |
|---|---|
| Long prompts cause time out. | Selection size is limited to avoid prompts from being too long. |
| Program used to freeze waiting for the AI's response. | The AI is executed in the background, displaying a thinking window to let the user know the process is being executed. |

---

#### Fix Conventions — `ConventionAction`

**Process:**
1. Validates selection against the configurable `maxSelectionChars` limit.
2. Pre-processes code to break lines exceeding `maxLineLength` at operators and commas.
3. Injects code into `conventionPrompt` with the `[MAX_LINE_LENGTH]` parameter.
4. Sends prompt to Ollama on a background thread.
5. Cleans response and displays a split-view preview highlighting modified lines with `← CHANGED:` markers.
6. Applies changes using `Document.replaceString()` within a `WriteCommandAction`.

**Challenges & Solutions:**

| Issue | Resolution |
|---|---|
| Initial implementations removed code segments to enforce character limits. | Implemented deterministic Java-side line breaking before AI processing. The LLM is restricted to naming corrections and safe formatting adjustments only. |
| Text replacement caused offset drift in larger files. | Restricted modifications to the exact original `TextRange` boundaries. All mutations execute within `WriteCommandAction` to ensure reliable undo behavior and editor state consistency. |

---

## Author

**Rafael Santiago De La Torre Jiménez**

*Built as a demonstration for the JetBrains internship application.*
