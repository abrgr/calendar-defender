# calendar-defender

## REPL
```
./scripts/repl
```

## Deployment
1. ./scripts/push
2. ./scripts/deploy ${rev from push}
3. ./scripts/deploy-status ${execution arn from deploy}

## Clojurescript issues

### Fail to properly deal with circular dependencies
[@mrblenny/react-flow-chart](https://github.com/MrBlenny/react-flow-chart) has imports like: `require('../..')`, where the referenced import obviously eventually includes the importer.  These requires are ignored when generating the goog.require statements in generated code.

Temporary solution: Break circular dependencies by directly requiring each component.

### Fail to properly deal with es6 spread imports
[styled-components](https://www.npmjs.com/package/styled-components) has imports like `import { isElement } from 'react-is'`.  The generated code for `isElement(x)` is `module$usr$src$app$node_modules$react_is$index.isElement(x)` when it should be `module$usr$src$app$node_modules$react_is$index['default'].isElement(x)`.

Temporary solution: Don't use spread imports.
