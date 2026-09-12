# Coding and comment conventions

Preserve the project's established style when updating or replacing code. Refactoring must not silently replace its comment conventions with a different style. Apply style changes to the code being worked on; avoid unrelated whole-project formatting.

When replacing or renaming a class, also consult its previous version in version control, when available. Preserve useful documentation, updating it to describe the new implementation accurately.

The Java guidance and examples below apply regardless of the project's directory layout or framework. For other languages, follow the project's established language-specific conventions.

## Working practices

- Preserve unrelated changes already in the working tree; do not overwrite or revert them.
- Keep changes focused on the task. Avoid unrelated refactoring, new dependencies, and public API changes unless needed for the task.
- On completion, briefly state what changed, what was checked, and any remaining limitations.

## Design and implementation

- Strive for simple, concise code that is easy to understand and maintain. Keep control flow easy to follow.
- Use blank lines to separate logical sections of code and make the flow easier to follow. Preserve useful existing spacing, keep closely related statements together, and avoid excessive blank lines.
- Follow established patterns in the project. Prefer familiar Java constructs; avoid exotic language techniques or clever abstractions when a straightforward solution is sufficient.
- Avoid unnecessary helper methods and layers of indirection. Extract a helper when it meaningfully clarifies the code, reduces duplication, or supports reuse.
- Make code easy to use and reuse through clear names, straightforward interfaces, and minimal coupling. Avoid speculative generalisation for needs the project does not have.
- Take appropriate steps to achieve strong performance in likely performance-sensitive areas. Choose suitable algorithms and data structures, avoid unnecessary repeated work and allocations in frequently executed paths, and measure when performance trade-offs are uncertain. Keep optimisations focused and explain any non-obvious complexity they require.

## Existing conventions

- Use the project's configured source formatter for mechanical formatting.
- Retain the file's naming conventions, including existing `m_` field names. Renamed or replacement classes inherit their predecessor's field naming conventions; meaningful field renames must retain the existing prefix. Do not rename variables solely to impose another convention.
- Preserve existing licence headers. Follow `.gitattributes` for line endings where specified; otherwise preserve the file's established convention. If a file already has mixed line endings, use untouched portions and neighbouring files to determine the intended convention.

## Comments and documentation

- Preserve existing three-line asterisk class banners for top-level Java classes and add them to new top-level Java classes, with a short description of each class's purpose. Nested classes, records, interfaces, and enums must not have banners; use a single-line comment only when helpful and appropriate.
- Preserve existing asterisk separators and add one before each new Java constructor and method, labelled with `constructor` or the method name. Place the separator before any associated comments, Javadoc, and annotations. When renaming a method, update its separator label to match; constructor labels remain `constructor`. Match the surrounding separator width and alignment, or the embedded examples in new files.
- Use short trailing field comments describing each field's purpose.
- Use concise comments for logical blocks, normally beginning with a lowercase word, as in `// construct data model` or `// revert command` in Java.
- Explain non-obvious validation rules, object identity, undo behaviour, and ordering constraints. Retain these explanations when moving the associated code.
- Do not introduce new Javadoc unless explicitly asked to add it. Preserve and update useful existing Javadoc when moving or replacing code; this is maintenance of existing documentation, not new Javadoc. Fix errors and mistakes in existing Javadoc.
- Keep source comments focused on the code; omit conversational history and temporary implementation notes.

## Embedded examples

### Class banner, field comments, and method separators

Place the class banner after the imports. Use descriptive field comments and label each constructor and method separator as illustrated below.

```java
/*************************************************************************************************/
/******************************** Collection of names for lookup *********************************/
/*************************************************************************************************/

public class NameCollection
{
  private final List<String> m_names; // names available for lookup

  /**************************************** constructor ******************************************/
  public NameCollection( List<String> names )
  {
    // copy names so later changes to the supplied list do not affect lookup
    m_names = List.copyOf( names );
  }

  /******************************************* contains ******************************************/
  public boolean contains( String name )
  {
    return m_names.contains( name );
  }
}
```

### Validation, undo, and ordering comments

Explain the reason for a constraint or sequence beside the relevant code. For example, an undo operation may need saved values to restore the original state. This method excerpt assumes an undo interface and an existing `apply` method:

```java
  /******************************************* undo **********************************************/
  @Override
  public void undo()
  {
    // revert command using the saved values to restore the original state
    apply( oldValues );
  }
```

Other useful block-comment patterns include:

```java
// validate all entries before changing the collection to avoid partial updates
// restore the same objects so existing references remain valid
// insert from highest to lowest index to avoid shifting remaining insertion positions
// reserve supplied names first so generated defaults cannot take a later entry's name
// refresh the view after the current edit has finished committing
```
