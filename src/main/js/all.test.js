import { expect, test } from 'vitest'

test("setTimeout is present", () => {
  expect(typeof setTimeout).toBe("function");
});
