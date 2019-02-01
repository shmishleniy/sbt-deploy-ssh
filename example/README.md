# Example

This example uses native-packager and just prints its config.

```bash
ssh-copy-id localhost
sbt
> deploySsh server1
cat /tmp/example/nohup.out
# should be 'name1'
```
