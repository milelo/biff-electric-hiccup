# Biff-Electric example

Example of using [Electric](https://github.com/hyperfiddle/electric) with Biff.

This fork has been modified to use [electric-hiccup]. See [app.cljc].

### HTML Styling
This project uses [tailwindcss] for HTML styling in [app.cljc].
I've included [settings](/.vscode/settings.json) for the [Tailwind CSS IntelliSense] plugin for VSCode to provide class autocomplete and tooltip css definitions for electric-hiccup. If you are using VSCode install the plugin to enable the features.

Other implementations of [app.cljc] for comparison:

* The original: [app.cljc][biff-electric app.cljc] from [biff-electric].
* A htmx version: [app.clj][starter/src/com/example/app.clj] from the main [biff] repo.

## Inherited README information:

Run `bb dev` to get started. See `bb tasks` for other commands.

View [the latest commit](https://github.com/jacobobryant/biff-electric/commits/master)
to see how this differs from the regular Biff example app. To use Electric in
your own Biff project, it's recommended to create a Biff app
[the normal way](https://biffweb.com/docs/get-started/new-project/) and then
manually apply the changes in this project's latest commit. This ensures that
your project will be created with the latest version of Biff—this repo won't
necessarily be upgraded to future Biff releases.

[electric-hiccup]:https://github.com/milelo/electric-hiccup
[app.cljc]: /src/com/biffweb/examples/electric/app.cljc
[biff-electric]: https://github.com/jacobobryant/biff-electric
[biff-electric app.cljc]: https://github.com/jacobobryant/biff-electric/blob/master/src/com/biffweb/examples/electric/app.cljc
[biff]: https://github.com/jacobobryant/biff
[starter/src/com/example/app.clj]: https://github.com/jacobobryant/biff/blob/28afb31cfcafc59f8e60ad32066ac0bb58691f7c/starter/src/com/example/app.clj
[Tailwind CSS IntelliSense]: https://marketplace.visualstudio.com/items?itemName=bradlc.vscode-tailwindcss
[tailwindcss]: https://tailwindcss.com
