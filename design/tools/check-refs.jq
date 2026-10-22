[inputs] as $s
| [range(0; $s|length) as $i
   | $s[$i] | [.. | objects | select(has("$step")) | .["$step"]][] as $r
   | select($r >= $i or ($s[$r].command | test("create|import|add$|combine|addVariant|slotInsert") | not))
   | "step \($i) (\($s[$i].command)) -> \($r) (\($s[$r].command // "none"))"]
| if length == 0 then empty else .[] end
