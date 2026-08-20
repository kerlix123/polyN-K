def twoSum(nums: list[int], target: int) -> list[int]:
	seen = {}
	for i in range(0, (len(nums) - 1)+ 1, 1):
		diff = (target - nums[i])
		if (diff in seen):
			return [seen[diff], i]
		
		seen[nums[i]] = i
	
a = twoSum([2, 7, 11, 15], 9)
print(a)
