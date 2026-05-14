package rsdltj.myplugin.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/*
    We serialize an object with all the required settings to connect the plugin to our AI
    This way the settings can be saved between uses, without manually more external files
*/
@State(
        name = "OllamaPluginSettings",
        storages = @Storage("OllamaPluginSettings.xml")
)

//This is the information we want to save
public class OllamaSettings implements PersistentStateComponent<OllamaSettings> {
    //We need the route to "contact" our local AI and the AI version
    public String ollamaUrl = "http://localhost:11434";
    public String modelName = "llama3.2:latest";

    // This prompt is the one we use for the suggested code with comments in between lines
    public String explanationPrompt = """
        You are a senior Java developer mentoring our team. Add exactly one `//` comment directly ABOVE each line of code. The comment MUST appear on the line immediately BEFORE the code line it describes. Follow these rules STRICTLY:
            POSITIONING (CRITICAL):
            - Comment line → Code line → [optional blank line] → Next comment line
            - NEVER place a comment after, below, or on the same line as the code it describes.
            - If a code line starts with `public`, `class`, `void`, `System`, etc., the comment goes on the line directly above that first character.
    
            CONTENT RULES:
            1. Use "we", "our", "us" for a collaborative tone.
            2. Explain WHAT each method/class does and WHY we use it. Assume we know basic syntax but not the specific syntax in this code.
            3. Leave exactly one empty line between a code line/block and the NEXT comment (not between code and its own comment).
            4. PRESERVE THE EXACT ORIGINAL CODE: Do NOT add, remove, or change any whitespace, line breaks, indentation, or code tokens. Only insert new // comment lines.
            5. Return ONLY raw Java code with comments. No markdown, no backticks, no explanations, no extra text. NO code fences (```), NO language tags, NO tutorials, NO additional classes or methods.
            6. Comment ONLY the provided snippet. Do NOT generate any code beyond what is shown in the input. This includes extra comments after the original code. Assume with the comments on top of each line will be enough explanation.
            7. GENERATE NEW COMMENTS SPECIFIC TO THE PROVIDED CODE. Do NOT copy, paraphrase, or reuse any text from the examples below. Each comment must explain the actual code it precedes.
    
            Example input (for positioning reference ONLY):
            public static void main(String[] args) {
                System.out.println("Hello World");
            }
            Example output (study the EXACT order: comment → code → blank → comment → code):
            // [Example only: explains main method execution flow]
            public static void main(String[] args) {
    
                // [Example only: explains println behavior with strings]
                System.out.println("Hello World");
            }
    
            WRONG: Copying example comments verbatim:
            // This tells our application to execute the code inside its braces when we run the program.   ← Example text copied!
            System.out.println(number);   ← Wrong code, wrong comment!
    
            WRONG: Comment AFTER code (this is the bug we're fixing):
            public static void main(String[] args) {
                System.out.println("Hello World"); // This prints...   ← WRONG: comment should be BEFORE
            }
    
            WRONG: Comment on next line below code:
            public static void main(String[] args) {
                System.out.println("Hello World");
                // This prints...   ← WRONG: comment should be ABOVE, not below
            }
    
            WRONG: Comment inside braces instead of before keyword:
            public static void main(String[] args) {
              // This tells our application...   ← WRONG: should be BEFORE "public"
                System.out.println("Hello World");
            }
    
            WRONG: Missing comment:
            public static void main(String[] args) {
                System.out.println("Hello World");  ← Missing comment above this line!
            }
   
            WRONG: Extra content:
            ```java
            // comment
            public static void main...
            // ...extra classes, tutorials, or explanations...
    
           Now process this code EXACTLY as shown. For EVERY line with code, add a comment on the line DIRECTLY ABOVE it. Return ONLY the final commented code:
                %s
    """.stripIndent();

    // This prompt is the one to generate a single comment paragraph to include right before the selection
    public String blockCommentPrompt = """
            You are a senior Java developer mentoring our team. Explain the selected code by generating a SINGLE /* */ block comment that contains all explanations. Follow these rules STRICTLY:
        
            FORMAT RULES:
            1. Start with /* and end with */
            2. Put the entire explanation as a flowing, coherent paragraph inside the block, with each visual line prefixed with " * " (space-asterisk-space)
            3. Use line breaks only for readability (max ~80 chars per line), NOT to separate distinct explanation points
            4. Do NOT include any code in the output — only the /* */ comment block
            5. NEVER use bracketed headers like [Explanation of X] or [Example only: ...] — write natural sentences only
            CONTENT RULES:
            6. Use "we", "our", "us" for a collaborative tone.
            7. Explain WHAT the code does and WHY we use it. Assume we know basic syntax but not the specific syntax in this code.
            8. Write as ONE connected paragraph: use transition words like "first", "then", "next", "finally" to link ideas naturally
            9. Explain in the same order the code appears, but weave explanations together — do NOT treat each line as a separate bullet point
        
            Example input:
            public static void main(String[] args) {
                System.out.println("Hello World");
            }
        
            Example output (study the flowing, connected style):
            /*
             * This is where our application starts execution: we define the entry point of our program with the main method.
             * Then, we use System.out.println to print text to the console — here it outputs "Hello World", a common greeting
             * used to verify our program runs correctly.
             */
        
            WRONG: Placeholder headers (this is the bug we're fixing):
            /*
             * [Explanation of the main method]
             *
             * This is where our application starts...
             */
        
            WRONG: Disconnected bullet points:
            /*
             * This defines the main method.
             *
             * This prints "Hello World".
             */
        
            WRONG: Multiple // comments:
            // This tells our application...
            public static void main...
        
            WRONG: Missing block format:
            This tells our application...
       
            Now generate a SINGLE /* */ block comment explaining this code as ONE flowing paragraph. Return ONLY the commented block:
            %s
        """;

    // This is the prompt we use to ask the AI to check for convention errors
    public String conventionPrompt = """
        You are a senior Java developer. Review the Java code below and fix ONLY convention violations. Follow these rules STRICTLY:

        CONVENTIONS (per Oracle Java Code Conventions):
        1. Classes/Interfaces: PascalCase (e.g., UserProfile, DataProcessor)
        2. Methods/Variables: camelCase (e.g., calculateTotal, userList)
        3. Constants (static final): UPPER_SNAKE_CASE (e.g., MAX_RETRIES, DEFAULT_TIMEOUT)
        4. Packages: lowercase with dots (e.g., com.example.utils)
        5. Acronyms in names: Treat as words (e.g., parseXml(), not parseXML())
        6. Line Length: MAX [MAX_LINE_LENGTH] characters.
           - For CODE: Break long lines at natural points (commas, operators, before arguments). Indent continuations by 4 spaces.
           - For COMMENTS: Break long comment lines at spaces or after punctuation (.,;:). Preserve all original words and meaning.
           - NEVER delete, truncate, or omit content to satisfy line length. Always preserve the full original text.
           - Java syntax integrity ALWAYS overrides line length. Never break logic or compilation.

        COMMENT HANDLING (IMPORTANT):
        - Fix naming conventions INSIDE comments too (e.g., "my_variable" → "myVariable" in comment text).
        - Break long comment lines at natural points to meet the character limit, but NEVER delete words.
        - Preserve the comment style (//, /*, /**) and structure.
        - Example:
          INPUT (150 chars):  // This method uses my_bad_name and USER_input_value for testing the convention checker plugin
          OUTPUT (broken):    // This method uses myBadName and userInputValue for testing the convention checker
                              //     plugin
          ← Note: Names fixed, line broken at space, NO words deleted.

        RULES:
        - Return ONLY the corrected Java code. No explanations, no markdown, no extra text.
        - Preserve ALL logic, comments, formatting, and structure. Only change identifiers that violate conventions.
        - If a name is already correct, leave it unchanged.
        - Do NOT add, remove, or reorder imports, methods, or classes.
        - This is a user-selected code region. Only modify the provided code; do not assume or generate surrounding context.

        Code to review:
        %s
        """;

    // Max line length for convention checking
    public int maxLineLength = 120;

    // Max characters allowed in selection for Convention Checker
    public int maxSelectionChars = 6000;

    // This is the text displayed on a window while our AI thinks, it can be changed in the settings
    public String thinkingMessage = "Analyzing your code...";

    //With this we can tell if the user marked the "don't show again" checkbox to hide the explanation text box
    public boolean showSetupHelp = true;

    //With the getter the app can load the current state to save before closing
    @Nullable
    @Override
    public OllamaSettings getState() {
        return this;
    }

    /*
        This constructor is meant to create an object based on the persistent state saved on the XML
        which "loads" the settings to the current instance of the app
    */
    @Override
    public void loadState(@NotNull OllamaSettings state) {
        this.ollamaUrl = state.ollamaUrl;
        this.modelName = state.modelName;
        this.showSetupHelp = state.showSetupHelp;
        this.maxLineLength = state.maxLineLength;
        this.maxSelectionChars = state.maxSelectionChars;
    }

    /*
      in case the user applied changes and wants to revert them, this resets all settings and prompts to their original default values.
      Creates a fresh instance to grab the initial hardcoded defaults, then copies them over.
     */
    public void resetToDefaults() {
        OllamaSettings defaults = new OllamaSettings();
        this.ollamaUrl = defaults.ollamaUrl;
        this.modelName = defaults.modelName;
        this.explanationPrompt = defaults.explanationPrompt;
        this.blockCommentPrompt = defaults.blockCommentPrompt;
        this.conventionPrompt = defaults.conventionPrompt;
        this.maxLineLength = defaults.maxLineLength;
        this.maxSelectionChars = defaults.maxSelectionChars;
        this.thinkingMessage = defaults.thinkingMessage;
        this.showSetupHelp = defaults.showSetupHelp;
    }
}