# JetBrains Marketplace checklist

- Replace the placeholder vendor name, email, and URL in `plugin.xml`.
- Add `pluginIcon.svg` and `pluginIcon_dark.svg` under `src/main/resources/META-INF/`.
- Choose and reserve the final plugin ID before publishing.
- Run `gradle verifyPlugin`.
- Run `gradle buildPlugin` and test installation from disk.
- Test first-use onboarding, multiple proto roots, imported protos, ambiguous message names, and large BLOB values.
- Configure signing and Marketplace publishing credentials in CI.
- Review the privacy policy requirement. The plugin does not transmit database values or source files.
