-- ============================================
-- Scientific Math plugin for Plugin Calculator
-- ============================================

local function isInteger(n)
	return n == math.floor(n)
end

local function factorial(n)
	if not isInteger(n) then
		error("factorial is only defined for integers")
	end
	if n < 0 then
		error("factorial is not defined for negative numbers")
	end
	local result = 1
	for i = 2, n do
		result = result * i
	end
	return result
end

local function nCr(n, r)
	if not isInteger(n) or not isInteger(r) then
		error("nCr is only defined for integers")
	end
	if n < 0 or r < 0 then
		error("nCr is not defined for negative numbers")
	end
	if r > n then
		error("nCr requires r <= n")
	end
	r = math.min(r, n - r)
	local result = 1
	for i = 0, r - 1 do
		result = result * (n - i) / (i + 1)
	end
	return result
end

-- init operation list
local operations = {}

-- nullary

operations["pi"] = function(args)
	return math.pi
end

operations["e"] = function(args)
	return math.exp(1)
end

-- unary

operations["factorial"] = function(args)
	return factorial(args[1])
end

operations["sqrt"] = function(args)
	if args[1] < 0 then
		error("sqrt is not defined for negative numbers")
	end
	return math.sqrt(args[1])
end

operations["abs"] = function(args)
	return math.abs(args[1])
end

-- binary

operations["power"] = function(args)
	return args[1] ^ args[2]
end

operations["mod"] = function(args)
	if args[2] == 0 then
		error("mod by zero is not allowed")
	end
	return math.fmod(args[1], args[2])
end

operations["log"] = function(args)
	local x, base = args[1], args[2]
	if x <= 0 then
		error("log is not defined for non-positive numbers")
	end
	if base <= 0 or base == 1 then
		error("log base must be positive and different from 1")
	end
	return math.log(x) / math.log(base)
end

operations["nCr"] = function(args)
	return nCr(args[1], args[2])
end

function execute(operation, args)
	local handler = operations[operation]
	if handler == nil then
		return { type = "error", message = "Unknown operation: " .. tostring(operation) }
	end

	local ok, result = pcall(handler, args)
	if ok then
		return result
	end

	return { type = "error", message = tostring(result) }
end