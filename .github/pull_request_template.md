name: Pull Request
about: Submit a pull request
title: ''
labels: ''
assignees: ''
body:
  - type: markdown
    attributes:
      value: |
        Thanks for your contribution! Please fill out this template.

  - type: dropdown
    id: type
    attributes:
      label: Type of change
      options:
        - feat (new feature)
        - fix (bug fix)
        - docs (documentation)
        - refactor (code restructuring)
        - perf (performance)
        - test (tests)
        - chore (maintenance)
        - translation (i18n)
        - ci (CI/CD)
    validations:
      required: true

  - type: textarea
    id: description
    attributes:
      label: Description
      description: What does this PR do and why?
    validations:
      required: true

  - type: textarea
    id: testing
    attributes:
      label: Testing
      description: How was this tested? What devices/Android versions?
      placeholder: |
        - Tested on Pixel 7 (Android 14), Xiaomi Redmi (MIUI 14)
        - ./gradlew detekt passes
        - ./gradlew test passes

  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: I have read the [CONTRIBUTING.md](CONTRIBUTING.md)
          required: true
        - label: My code follows the project's code style
          required: true
        - label: I have run `./gradlew detekt` and all checks pass
          required: true
        - label: I have tested my changes on at least one device
          required: true
        - label: My commit messages follow Conventional Commits
          required: true

  - type: input
    id: related_issue
    attributes:
      label: Related issue
      description: Link to the issue this PR addresses (if any)
      placeholder: "Closes #123"